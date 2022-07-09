package cl.puc.ing.edgedewsim.seas.node.jobstealing;

import cl.puc.ing.edgedewsim.mobilegrid.network.UpdateMessage;
import cl.puc.ing.edgedewsim.mobilegrid.node.*;
import cl.puc.ing.edgedewsim.seas.proxy.jobstealing.StealerProxy;
import cl.puc.ing.edgedewsim.simulator.Event;
import cl.puc.ing.edgedewsim.simulator.Simulation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JSDevice extends Device {

    public JSDevice(String name, BatteryManager bt, ExecutionManager em,
                    NetworkEnergyManager nem, Simulation simulation) {
        super(name, bt, em, nem, simulation);
    }

    public JSDevice(String name, BatteryManager bt, ExecutionManager em,
                    NetworkEnergyManager nem, Simulation simulation, boolean useBattery) {
        super(name, bt, em, nem, simulation, useBattery);
    }

    @Override
    public void startTransfer(@NotNull Node dst, int id, @Nullable Object data) {
        if (data instanceof UpdateMessage) {
            this.networkEnergyManager.onSendData(this, dst, UpdateMessage.STATUS_MSG_SIZE_IN_BYTES, UpdateMessage.STATUS_MSG_SIZE_IN_BYTES);
        } else {
            super.startTransfer(dst, id, data);
        }


	    /*
		if (data instanceof Job){		
			Job job = (Job) data;
			this.incomingJobTransfers.put(id, new JobTransfer(job, Simulation.getTime(), dst));

			int msgSize = dst.runsOnBattery()? job.getInputSize() : job.getOutputSize(); //if dst is another device then the data to be sent is the job not the job result.
			Message msg = new Message(id, this, dst, data, msgSize);
			this.networkEnergyManager.onSendData(msg);			
		} else if (data instanceof UpdateMsg) {
            Message msg = new Message(id, this, dst, data, UpdateMsg.STATUS_MSG_SIZE_IN_BYTES);
            this.networkEnergyManager.onSendData(msg);
		}
		*/
    }

    @Override
    public void processEvent(@NotNull Event event) {
        super.processEvent(event);
        if (event.getEventType() == EVENT_TYPE_BATTERY_UPDATE) {
            ((StealerProxy) SchedulerProxy.getProxyInstance(simulation)).steal(this);
        }
    }
}
