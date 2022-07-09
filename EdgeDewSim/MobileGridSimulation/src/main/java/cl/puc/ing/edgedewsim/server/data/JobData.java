package cl.puc.ing.edgedewsim.server.data;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class JobData implements Data {
    long ops;
    int inputSize;
    int outputSize;

    public long getOps() {
        return ops;
    }

    public void setOps(long ops) {
        this.ops = ops;
    }

    public int getInputSize() {
        return inputSize;
    }

    public void setInputSize(int inputSize) {
        this.inputSize = inputSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobData jobData = (JobData) o;
        return ops == jobData.ops &&
                inputSize == jobData.inputSize &&
                outputSize == jobData.outputSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ops, inputSize, outputSize);
    }

    @Override
    public void printData(@NotNull DataOutputStream outputStream) throws IOException {
        outputStream.writeLong(ops);
        outputStream.writeInt(inputSize);
        outputStream.writeInt(outputSize);
        outputStream.flush();
    }
}
