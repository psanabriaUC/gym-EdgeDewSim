package cl.puc.ing.edgedewsim.server.data;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public interface Data {
    void printData(@NotNull DataOutputStream outputStream) throws IOException;
}
