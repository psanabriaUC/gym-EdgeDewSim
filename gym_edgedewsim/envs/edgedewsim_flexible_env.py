import gym
import socket
import numpy as np
from struct import pack, unpack
import subprocess
import fcntl
import time

INT_LENGTH = 4
LONG_LENGTH = 8
BOOLEAN_LENGTH = 1
FLOAT_LENGTH = 4
DOUBLE_LENGTH = 8
BYTEORDER = 'big'
NEXT_MESSAGE_CODE = 2
RESET_CODE = 0
DEVICE_FIRST_DATA_LENGTH = 41  # id_name has to have fixed size of 36
DEVICE_DATA_LENGTH = 24
JOB_DATA_LENGTH = 16
STATS_DATA_LENGTH = 20
DEVICES_BUFF_SIZE = 5
MODES = {
    "uniform": 100,
    "all": 101,
}

class EdgeDewSimFlexibleEnv(gym.Env):
    metadata = {'render.modes': ['human']}

    def __init__(self,
                 host='localhost',
                 port=3000,
                 cnf_path='sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-RJOBS.cnf',
                 min_jobs=800,
                 max_jobs=1200,
                 min_ops=500,
                 max_ops=25000,
                 min_input=500,
                 max_input=25000,
                 min_output=500,
                 max_output=25000,
                 start_time=100,
                 min_delta=5,
                 max_delta=50,
                 mode="uniform",
                 ):
        """
        :param host: direccion del host
        :param port: puerto del host
        :param min_jobs: La cantidad minima de Jobs que va a generar
        :param max_jobs: La cantidad máxima de Jobs que va a generar
        :param min_ops: La cantidad minima de OPS para los Jobs que va a generar
        :param max_ops: La cantidad máxima de OPS para los Jobs que va a generar
        :param min_input: El tamaño mínimo para el inputSize de Jobs que va a generar
        :param max_input: El tamaño máximo para el inputSize de Jobs que va a generar
        :param min_output: El tamaño mínimo para el outputSize de Jobs que va a generar
        :param max_output: El tamaño máximo para el outputSize de Jobs que va a generar
        :param start_time: A partir de que instante van a llegar los jobs en la simulación
        :param min_delta: Mínimo rango de tiempo extra que se le va a dar al siguiente job
        :param max_delta: Máximo rango de tiempo extra que se le va a dar al siguiente job
        """

        self.server_address = (host, port)
        self.cnf_path = cnf_path
        self.min_jobs = min_jobs
        self.max_jobs = max_jobs
        self.min_ops = min_ops
        self.max_ops = max_ops
        self.min_input = min_input
        self.max_input = max_input
        self.min_output = min_output
        self.max_output = max_output
        self.start_time = start_time
        self.min_delta = min_delta
        self.max_delta = max_delta
        self.mode = MODES[mode]

        self.done = False
        self.simulation_id = None

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.sock.connect(self.server_address)

        message = self.cnf_path.encode()
        message_length = pack('>i', len(message))

        self.sock.sendall(message_length)
        self.sock.sendall(message)

        self._send_jobs_first_info()
        self._send_jobs_repeated_info()

        devices, job, completed_jobs = self._receive_first_step_info()
        self.devices = [device[0] for device in devices]
        self.action_space = gym.spaces.Discrete(len(self.devices))
        self.observation_space = gym.spaces.Dict({
            'devices': gym.spaces.Box(low=0, high=float('inf'), shape=(len(self.devices), 4)),
            'job': gym.spaces.Box(low=0, high=float('inf'), shape=(3,)),
            'completed_jobs': gym.spaces.Box(low=0, high=float('inf'), shape=(5,))})
        self.state = [devices, job, completed_jobs]
        self.first_run = True

    def step(self, action):
        """
        Send one action to simulator and receive data from next state
        :param action: index of device to select from self.state[0] list of devices
        :return: state, reward, done, info
        """
        device_id = self.devices[action]
        message_code = pack('i', NEXT_MESSAGE_CODE)
        message = pack('>q', device_id)

        self.sock.sendall(message_code)
        self.sock.sendall(message)

        devices, job, completion_info = self._receive_step_info()
        # reward = completion_info[1] - self.state[2][1] # jobs completed
        reward = float(completion_info[2]) / float(completion_info[3] * 1000000) if self.done else 0.0  # ops/secs
        # reward = completion_info[5] # AVG({ops/secs | per job})
        # reward = completion_info[4] - self.state[2][4]  # Difference in SUM({ops/secs | per job})
        self.state = [devices, job, completion_info]

        # option 1: scale the reward signal
        reward = 10 * reward
        # option 2: reward only if the agent does better than random
        # reward = 10*reward if reward > 0.7 else 0.0
        # option 3: scale the reward signal
        # reward = reward/1000000

        # giving all the reward in the last step
        self.total_reward += reward
        # reward = self.total_reward if self.done else 0.0
        # reward = (self.total_reward - 19) if self.done else 0.0

        return self._get_observation(*self.state), reward, self.done, {}

    def reset(self):
        """
        Send code to reset simulation
        Receive first state of devices and job and save number of devices and ids
        Return first state
        :return: state
        """
        self.total_reward = 0.0
        if self.first_run:
            self.first_run = False
            return self._get_observation(*self.state)

        self.done = False
        message = pack('i', RESET_CODE)
        self.sock.sendall(message)

        self._send_jobs_repeated_info()

        devices, job, completed_jobs = self._receive_first_step_info()

        self.state = [devices, job, completed_jobs]
        return self._get_observation(*self.state)

    def _get_observation(self, devices, jobs, completed_jobs):
        if jobs is None:
            jobs = [0, 0, 0]
        devices = [d[3:] for d in devices]  # Removing the device's id since it is an irrelevant feature
        return dict(devices=np.array(devices), job=np.array(jobs), completed_jobs=np.array(completed_jobs))

    def _receive_step_info(self):
        """
        Receive data from remote simulator until scheduler asks for action
        Returns info from devices and current job
        If simulator stops sending data, returns no info from devices and current job and returns done as True
        :return: devices_info:list, job_info:list, completion_info:list, done:bool
        devices_info = [id:str,
                        mips:int,
                        n_jobs:int,
                        remaining_battery:int,
                        runs_in_battery:bool,
                        cpu_usage:float,
                        assigned_ops:int]
        job_info = [job_ops:int, job_input_size:int, job_output_size:int]
        completion_info = [total_jobs:int,
                           completed_jobs: int,
                           total_job_ops: int,
                           timestamp: int,
                           sum_success: float]
        """
        devices = None
        job = None
        command = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        if command == 2:
            num_devices = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)

            num_iterations = num_devices // DEVICES_BUFF_SIZE

            devices = []
            for iteration in range(num_iterations):
                data = self._receive_bytes(DEVICES_BUFF_SIZE * DEVICE_DATA_LENGTH)

                for index in range(DEVICES_BUFF_SIZE):
                    i = index * DEVICE_DATA_LENGTH
                    n_jobs = int.from_bytes(data[i: i + INT_LENGTH], BYTEORDER)
                    i += INT_LENGTH
                    remaining_battery = int.from_bytes(data[i: i + INT_LENGTH], BYTEORDER)
                    i += INT_LENGTH
                    cpu_usage = unpack('d', data[i: i + DOUBLE_LENGTH][::-1])[0]
                    i += DOUBLE_LENGTH
                    assigned_ops = int.from_bytes(data[i: i + LONG_LENGTH], BYTEORDER)
                    i += LONG_LENGTH

                    device_saved_data = self.state[0][iteration * DEVICES_BUFF_SIZE + index]
                    device = [
                        device_saved_data[0],
                        device_saved_data[1],
                        n_jobs,
                        remaining_battery,
                        cpu_usage,
                        device_saved_data[6],
                        assigned_ops,
                    ]
                    devices.append(device)

            job_ops = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
            job_input_size = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
            job_output_size = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)

            job = (job_ops, job_input_size, job_output_size)
        else:
            self.done = True

        total_jobs = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        completed_jobs = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        total_job_ops = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        timestamp = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        sum_success = unpack('f', self._receive_bytes(FLOAT_LENGTH)[::-1])[0]

        stats = (total_jobs, completed_jobs, total_job_ops, timestamp, sum_success)

        if devices is None:
            devices = self.state[0]

        return devices, job, stats

    def _receive_first_step_info(self):
        """
        Receive data from remote simulator until scheduler asks for action
        Returns info from devices and current job
        If simulator stops sending data, returns no info from devices and current job and returns done as True
        :return: devices_info:list, job_info:list, completion_info:list, done:bool
        devices_info = [id:str,
                        mips:int,
                        n_jobs:int,
                        remaining_battery:int,
                        runs_in_battery:bool,
                        cpu_usage:float,
                        assigned_ops:int]
        job_info = [job_ops:int, job_input_size:int, job_output_size:int]
        completion_info = [total_jobs:int,
                           completed_jobs: int,
                           total_job_ops: int,
                           timestamp: int,
                           sum_success: float]
        """
        less_significant = self._receive_bytes(LONG_LENGTH)
        more_significant = self._receive_bytes(LONG_LENGTH)
        hex_id = (more_significant + less_significant).hex()
        self.simulation_id = "{}-{}-{}-{}-{}".format(hex_id[:8], hex_id[8:12], hex_id[12:16], hex_id[16:20],
                                                     hex_id[20:])

        devices = None
        job = None
        command = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        if command == 2:
            num_devices = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)

            num_iterations = num_devices // DEVICES_BUFF_SIZE

            devices = []
            for _ in range(num_iterations):
                data = self._receive_bytes(DEVICES_BUFF_SIZE * DEVICE_FIRST_DATA_LENGTH)

                for index in range(DEVICES_BUFF_SIZE):
                    i = index * DEVICE_FIRST_DATA_LENGTH
                    device_id = int.from_bytes(data[i: i + LONG_LENGTH], BYTEORDER)
                    i += LONG_LENGTH
                    mips = int.from_bytes(data[i: i + LONG_LENGTH], BYTEORDER)
                    i += LONG_LENGTH
                    n_jobs = int.from_bytes(data[i: i + INT_LENGTH], BYTEORDER)
                    i += INT_LENGTH
                    remaining_battery = int.from_bytes(data[i: i + INT_LENGTH], BYTEORDER)
                    i += INT_LENGTH
                    cpu_usage = unpack('d', data[i: i + DOUBLE_LENGTH][::-1])[0]
                    i += DOUBLE_LENGTH
                    runs_in_battery = unpack('?', data[i: i + BOOLEAN_LENGTH])[0]
                    i += BOOLEAN_LENGTH
                    assigned_ops = int.from_bytes(data[i: i + LONG_LENGTH], BYTEORDER)
                    i += LONG_LENGTH

                    device = [
                        device_id,
                        mips,
                        n_jobs,
                        remaining_battery,
                        cpu_usage,
                        runs_in_battery,
                        assigned_ops,
                    ]
                    devices.append(device)

            job_ops = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
            job_input_size = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
            job_output_size = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)

            job = (job_ops, job_input_size, job_output_size)
        else:
            self.done = True

        total_jobs = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        completed_jobs = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        total_job_ops = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        timestamp = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        sum_success = unpack('f', self._receive_bytes(FLOAT_LENGTH)[::-1])[0]

        stats = (total_jobs, completed_jobs, total_job_ops, timestamp, sum_success)

        if devices is None:
            devices = self.state[0]

        return devices, job, stats

    def _receive_bytes(self, length):
        return self.sock.recv(length, socket.MSG_WAITALL)

    def _send_jobs_first_info(self):
        self.sock.sendall(pack('>i', self.min_jobs))
        self.sock.sendall(pack('>i', self.max_jobs))
        self.sock.sendall(pack('>q', self.min_ops))
        self.sock.sendall(pack('>q', self.max_ops))
        self.sock.sendall(pack('>i', self.min_input))
        self.sock.sendall(pack('>i', self.max_input))
        self.sock.sendall(pack('>i', self.min_output))
        self.sock.sendall(pack('>i', self.max_output))
        self.sock.sendall(pack('>i', self.mode))

    def _send_jobs_repeated_info(self):
        self.sock.sendall(pack('>q', self.start_time))
        self.sock.sendall(pack('>i', self.min_delta))
        self.sock.sendall(pack('>i', self.max_delta))

    def render(self, mode='human'):
        """
        Print state of environment: all statistics received from the simulator
        """
        pass

    def close(self):
        """
        Terminate simulator process
        """
        pass
