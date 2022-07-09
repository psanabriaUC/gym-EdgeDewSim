# gym-edgedewsim

To run the RL agent, you have to install the [OpenAI baselines](https://github.com/openai/baselines) library. Then, run:

```
python3 run.py --alg=<name of the RL algorithm> --env=<environment_id>
```

Some possibilities for the RL algorithm include 'ppo2', 'deepq', 'a2c', and 'acer'. For instance, the following command runs [PPO](https://arxiv.org/abs/1707.06347) on the edgedewsim-hybrid-1500-v0 environment:

```
python3 run.py --alg=ppo2 --env=edgedewsim-hybrid-1500-v0
```
