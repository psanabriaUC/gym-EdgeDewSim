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
DOUBLE_LENGTH = 8
BYTEORDER = 'big'
NEXT_MESSAGE_CODE = 2
RESET_CODE = 0
DEVICE_FIRST_DATA_LENGTH = 45  # id_name has to have fixed size of 36
DEVICE_DATA_LENGTH = 28
JOB_DATA_LENGTH = 16
STATS_DATA_LENGTH = 20
DEVICES_BUFF_SIZE = 10


class EdgeDewSimAuxEnv(gym.Env):
    metadata = {'render.modes': ['human']}

    def __init__(self, host='localhost', port=3000, cnf_path='sim_input/TEST-Remote-5A100-JOBS.cnf'):

        self.sim_process = None
        self.server_address = (host, port)
        self.cnf_path = cnf_path

        #self.launch_simulator(port)


        self.done = False
        self.simulation_id = None

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.sock.connect(self.server_address)

        message = self.cnf_path.encode()
        message_length = pack('i', len(message))

        self.sock.sendall(message_length)
        self.sock.sendall(message)

        devices, job, completed_jobs = self._receive_first_step_info()
        self.devices = [device[0] for device in devices]
        self.action_space = gym.spaces.Discrete(len(self.devices))
        # The observations only remember the actions taken so far
        #self.observation_space = gym.spaces.Box(low=0, high=1, shape=(1500,len(self.devices)), dtype=np.uint8)
        #self.observation_space = gym.spaces.Box(low=0, high=len(self.devices), shape=(1500,), dtype=np.uint8)
        self.observation_space = gym.spaces.Box(low=0, high=1, shape=(1500,7), dtype=np.uint8)
        self.state = [devices, job, completed_jobs]
        self.first_run = True


    def bin_array(self, num, m):
        """Convert a positive integer num into an m-bit bit vector"""
        return np.array(list(np.binary_repr(num).zfill(m))).astype(np.int8)

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
        reward = float(completion_info[3])/float(completion_info[4]*1000000) if self.done else 0.0 # ops/secs
        self.state = [devices, job, completion_info]

        # updating the features
        #self.features[self.time_step,action] = 1
        #self.features[self.time_step] = action
        self.features[self.time_step,:] = self.bin_array(action, 7)
        self.time_step += 1

        # option 1: scale the reward signal (running in aux)
        reward = 10*reward
        # option 2: reward only if the agent does better than random (running in aux2)
        #reward = 10*reward if reward > 0.7 else 0.0

        return self.features, reward, self.done, {}

    def reset(self):
        """
        Send code to reset simulation
        Receive first state of devices and job and save number of devices and ids
        Return first state
        :return: state
        """
        #self.features = np.zeros((1500,len(self.devices)), dtype=np.uint8)
        #self.features = np.zeros((1500,), dtype=np.uint8)
        self.features = np.zeros((1500,7), dtype=np.uint8)
        self.time_step = 0
        if self.first_run:
            self.first_run = False
            return self.features

        self.done = False
        message = pack('i', RESET_CODE)
        self.sock.sendall(message)

        devices, job, completed_jobs = self._receive_first_step_info()

        self.state = [devices, job, completed_jobs]
        return self.features

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
                        uptime:int,
                        n_jobs:int,
                        remaining_battery:int,
                        runs_in_battery:bool,
                        cpu_usage:float,
                        assigned_ops:int]
        job_info = [job_ops:int, job_input_size:int, job_output_size:int]
        completion_info = [total_jobs:int, completed_jobs: int, incomplete_jobs: int]
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
                    uptime = int.from_bytes(data[i: i + INT_LENGTH], BYTEORDER)
                    i += INT_LENGTH
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
                        uptime,
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
        incomplete_jobs = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        total_job_ops = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        timestamp = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        stats = (total_jobs, completed_jobs, incomplete_jobs, total_job_ops, timestamp)

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
                                uptime:int,
                                n_jobs:int,
                                remaining_battery:int,
                                runs_in_battery:bool,
                                cpu_usage:float,
                                assigned_ops:int]
                job_info = [job_ops:int, job_input_size:int, job_output_size:int]
                completion_info = [total_jobs:int, completed_jobs: int, incomplete_jobs: int]
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
                    uptime = int.from_bytes(data[i: i + INT_LENGTH], BYTEORDER)
                    i += INT_LENGTH
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
                        uptime,
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
        incomplete_jobs = int.from_bytes(self._receive_bytes(INT_LENGTH), BYTEORDER)
        total_job_ops = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        timestamp = int.from_bytes(self._receive_bytes(LONG_LENGTH), BYTEORDER)
        stats = (total_jobs, completed_jobs, incomplete_jobs, total_job_ops, timestamp)

        if devices is None:
            devices = self.state[0]

        return devices, job, stats

    def _receive_bytes(self, length):
        return self.sock.recv(length, socket.MSG_WAITALL)

    def render(self, mode='human'):
        """
        Print state of environment: all statistics received from the simulator
        """
        pass

    def close(self):
        """
        Terminate simulator process
        """
        if self.sim_process:
            self.sim_process.terminate()
            rm_process = subprocess.Popen(['rm', '-rf', 'EdgeDewSim/MobileGridSimulation/sim_input'])
            rm_process.wait()
            rm_process = subprocess.Popen(['rm', 'simrun.lock'])
            rm_process.wait()

    def launch_simulator(self, port):
        """
        If simulator is not running, run it.
        If another process started simulator, wait until it's receiving connections.
        :return:
        """
        try:
            with open('simrun.lock', 'w') as file:
                fcntl.flock(file, fcntl.LOCK_EX | fcntl.LOCK_NB)
                cp_process = subprocess.Popen(['cp', '-r', 'EdgeDewSim/sim_input', 'EdgeDewSim/MobileGridSimulation'])
                cp_process.wait()
                self.sim_process = subprocess.Popen(['./gradlew', 'run', '--args=\'{}\''.format(port)], cwd='EdgeDewSim/',
                                                    stdout=subprocess.PIPE)
                server_started = False
                while not server_started:
                    output = self.sim_process.stdout.readline()
                    if output == '' and self.sim_process.poll() is not None:
                        break
                    if output:
                        print(output)
                    if 'Server starting' in str(output):
                        file.write('Server starting\n')
                        server_started = True
                fcntl.flock(file, fcntl.LOCK_UN)
        except BlockingIOError:
            server_started = False
            file = open('simrun.lock')
            while not server_started:
                where = file.tell()
                line = file.readline()
                if not line:
                    time.sleep(1)
                    file.seek(where)
                else:
                    if 'Server starting' in line:
                        print('Server started')
                        server_started = True
