import gym
import gym_edgedewsim
import time
import traceback
import subprocess
import sys

def launch_simulator():
    #cp_process = subprocess.Popen(['cp', '-r', 'EdgeDewSim/sim_input', 'EdgeDewSim/MobileGridSimulation'])
    #cp_process.wait()
    sim_process = subprocess.Popen(['./gradlew', 'run', '--args=\'3000\''], cwd='EdgeDewSim/',
                                   stdout=subprocess.PIPE)
    server_started = False
    while not server_started:
        output = sim_process.stdout.readline()
        if output == '' and sim_process.poll() is not None:
            break
        if output:
            print(output)
        if 'Server starting' in str(output):
            server_started = True

    return sim_process

def close_simulator(sim_process):
    print("Terminating simulator process")
    sim_process.terminate()
    print("Done.")

sim_process = launch_simulator()

## Envs
#env = gym.make('edgedewsim-hybrid-30min-500kb-v0')
#env = gym.make('FEAT-edgedewsim-hybrid-1500-v0')
#env = gym.make('AUX-edgedewsim-hybrid-1500-v0')
#env = gym.make('edgedewsim-hybrid-1000-v0')
#env = gym.make('edgedewsim-hybrid-1500-v0')
env = gym.make('edgedewsim-hybrid-2000-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-1500-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-3000-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-4500-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-9000-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-1500-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-3000-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-4500-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-9000-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-1500-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-3000-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-4500-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-500kb-9000-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-1500-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-3000-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-4500-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-1mb-9000-25devices-v0')
#env = gym.make('edgedewsim-hybrid-30min-longCPU-25devices-v0')
#env = gym.make('edgedewsim-flexible-scheduler-25devices-v0')
#env = gym.make('edgedewsim-flexible-scheduler-v0')
#

n_epochs = 5
n = 0
sim_id = env.simulation_id

try:
    while n < n_epochs:
        total_reward = 0
        steps = 0
        now = time.time()
        state = env.reset()
        done = False
        #print("Devices:", env.action_space.n)
        while not done:
            a = env.action_space.sample()
            state, reward, done, info = env.step(a)
            #print(reward)
            steps += 1
            total_reward += reward

        n += 1
        print("Took {} steps in {}. The total reward was {}".format(steps, time.time() - now, total_reward))
        print("Steps per second: " + str(steps / (time.time() - now)))
        #input()
except (Exception, KeyboardInterrupt) as err:
    print("Simulation ID: {} has stopped with an error".format(sim_id))
    print("Error was {}".format(err))
    print(traceback.format_exc())
finally:
    close_simulator(sim_process)
