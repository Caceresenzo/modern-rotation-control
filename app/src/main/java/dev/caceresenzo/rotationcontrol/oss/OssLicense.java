package dev.caceresenzo.rotationcontrol.oss;

import lombok.Data;

@Data
public final class OssLicense {

    private final String libraryName;
    private final long startOffset;
    private final int length;

}