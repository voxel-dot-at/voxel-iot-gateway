package at.voxel.iotgw.lwm2m;

final class Constants  {
    // Private constructor prevents instantiation
    private Constants() {}

    // Private Object ID (Vendor-Specific Range: 10241 to 32768)
    public static final int OBJ_SMART_VALVE = 17912;

    // Private Resource IDs (Custom Range: 26241 to 32768)
    public static final int RES_VALVE_POSITION = 26241; // Float (0.0 to 100.0)
    public static final int RES_IS_OPEN         = 26242; // Boolean
    public static final int RES_VALVE_NAME      = 26243; // String
    public static final int RES_RESET_VALVE     = 26244; // Executable
}