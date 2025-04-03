package com.bobbyesp.utilities.mediastore.model

enum class FileDescriptorMode(val modeKey: String) {
    READ("r"),
    WRITE("w"),
    READ_WRITE("rw"),
    APPEND("wa"),
    APPEND_READ("rwa"),
    TRUNCATE("wt"),
    TRUNCATE_READ("rwt"),
    APPEND_TRUNCATE("wta"),
    APPEND_TRUNCATE_READ("rwta")
}