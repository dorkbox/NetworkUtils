package dorkbox.netUtil.jna.windows.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public
class LUID extends Structure {
    public int LowPart;
    public int HighPart;

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("LowPart", "HighPart");
    }
}
