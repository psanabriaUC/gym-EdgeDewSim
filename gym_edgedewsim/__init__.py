from gym.envs.registration import register

## Test dataset

register(
    id='edgedewsim-test-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
)

## Mixed Jobs Datasets (1500, 2500, 3500 Jobs)

### Mobile topology (100 devices)

register(
    id='edgedewsim-mobile-1500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-20A100-30GalaxyTab2-50L9-1500JOBS.cnf'}
)

register(
    id='edgedewsim-mobile-2500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-20A100-30GalaxyTab2-50L9-2500JOBS.cnf'}
)

register(
    id='edgedewsim-mobile-3500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-20A100-30GalaxyTab2-50L9-3500JOBS.cnf'}
)

### Hybrid topology (120 devices: 70 IOT and 50 mobile)

register(
    id='edgedewsim-hybrid-1000-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-1000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-1500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-1500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-2000-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-2000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-2500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-2500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-3500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-3500JOBS.cnf'}
)

## Video Datasets (not cropped)

### Hybrid topology (120 devices: 70 IOT and 50 mobile)

register(
    id='edgedewsim-hybrid-30min-500kb-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-30Min-500KB-JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-30Min-1MB-JOBS.cnf'}
)

## Cropped video datasets (1500, 3000, 4500 and 9000 JOBS)

### Hybrid topology (60 devices: 37 IOT and 23 mobile)

register(
    id='edgedewsim-hybrid-30min-1mb-1500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-1MB-1500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-3000-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-1MB-3000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-4500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-1MB-4500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-9000-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-1MB-9000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-1500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-500kb-1500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-3000-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-500kb-3000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-4500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-500kb-4500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-9000-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-6A100-8GalaxyTab2-9L9-37IOT-30Min-500kb-9000JOBS.cnf'}
)

### Hybrid topology (25 devices: 16 IOT and 9 mobile)

register(
    id='edgedewsim-hybrid-30min-500kb-1500-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-500kb-1500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-3000-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-500kb-3000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-4500-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-500kb-4500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-500kb-9000-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-500kb-9000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-1500-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-1MB-1500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-3000-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-1MB-3000JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-4500-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-1MB-4500JOBS.cnf'}
)

register(
    id='edgedewsim-hybrid-30min-1mb-9000-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-1MB-9000JOBS.cnf'}
)

## Long CPU datasets

### Hybrid topology (25 devices: 16 IOT and 9 mobile)

register(
    id='edgedewsim-hybrid-30min-longCPU-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimEnv',
    kwargs={'cnf_path': 'sim_input/test_rl/Test-Remote-5A100-2GalaxyTab2-2L9-16IOT-30Min-longCPU-JOBS.cnf'}
)

#######################################
# -------------------------------- AUX
register(
    id='AUX-edgedewsim-hybrid-1500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimAuxEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-1500JOBS.cnf'}
)

# -------------------------------- features
register(
    id='FEAT-edgedewsim-hybrid-1500-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimFeaturesEnv',
    kwargs={'cnf_path': 'sim_input/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-1500JOBS.cnf'}
)

#######################################
# --------------------------------- flexible scheduler
register(
    id='edgedewsim-flexible-scheduler-25devices-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimFlexibleEnv',
    kwargs={
        'min_jobs': 800,
        'max_jobs': 1200,
        'min_ops': 1000,
        'max_ops': 5901810722000,
        'min_input': 1048576,
        'max_input': 524288000,
        'min_output': 1048576,
        'max_output': 524288000,
        'start_time': 5,
        'min_delta': 30,
        'max_delta': 70
    }
)

register(
    id='edgedewsim-flexible-scheduler-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimFlexibleEnv',
    kwargs={
        'cnf_path': 'sim_input/test_rl/Test-Remote-10A100-15GalaxyTab2-25L9-70IOT-RJOBS.cnf',
        'min_jobs': 800,
        'max_jobs': 1200,
        'min_ops': 1000,
        'max_ops': 5901810722000,
        'min_input': 1048576,
        'max_input': 524288000,
        'min_output': 1048576,
        'max_output': 524288000,
        'start_time': 5,
        'min_delta': 30,
        'max_delta': 70
    }
)

register(
    id='edgedewsim-flexible-scheduler-25devices-300to500JOBS-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimFlexibleEnv',
    kwargs={
        'min_jobs': 300,
        'max_jobs': 500,
        'min_ops': 1000,
        'max_ops': 600000000000,
        'min_input': 1048576,
        'max_input': 524288000,
        'min_output': 1048576,
        'max_output': 524288000,
        'start_time': 5,
        'min_delta': 30,
        'max_delta': 70
    }
)

register(
    id='edgedewsim-flexible-scheduler-25devices-300to500JOBS-all-v0',
    entry_point='gym_edgedewsim.envs:EdgeDewSimFlexibleEnv',
    kwargs={
        'min_jobs': 300,
        'max_jobs': 500,
        'min_ops': 1000,
        'max_ops': 5901810722000,
        'min_input': 1048576,
        'max_input': 524288000,
        'min_output': 1048576,
        'max_output': 524288000,
        'start_time': 5,
        'min_delta': 30,
        'max_delta': 70,
        'mode': 'all',
    }
)
