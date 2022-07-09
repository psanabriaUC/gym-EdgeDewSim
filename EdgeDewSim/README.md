# EdgeDewSim

## Instructions for building and running.

1. Run `gradle build`. This will generate some files under a `build` folder.
    ```
    >MobileGridSimulation
        >build
            >classes
            >distributions
                >MobileGridSimulation.tar
                >MobileGridSimulation.zip
            >generated
            >...
        >src
        >build.gradle
    >MobileGrid
    >Simulator
    >sim_input
    ```
2. Locate and unzip `MobileGridSimulation.zip`
3. Copy the `sim_input/` folder and all its contents into the unzipped MobileGridSimulation folder. This new folder should look like this:
    ```
    >MobileGridSimulation
        >bin
            >MobileGridSimulation
            >MobileGridSimulation.bat
        >lib
        >sim_input
    ```
4. To run the simulator correctly, start the process from the `MobileGridSimulation/` folder by running `./bin/MobileGridSimulation` so future references to `sim_input/` are made from the root of that folder.
