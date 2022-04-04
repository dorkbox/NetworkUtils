package dorkbox.netUtil

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

object IPRoute {
    /**
     * Gets the version number.
     */
    const val version = Common.version

    private val reservedTable = StringBuilder(2048)
    private val tableNames: MutableMap<Int, String?> = HashMap(256)

    /**
     * @throws IOException if the policy routing tables are unable to initialize
     */
    fun addRtTables(tableNames: Map<Int, String>) {
        if (Common.OS_LINUX) {
            for ((tableNumber, tableName) in tableNames) {
                if (tableNumber == 0 || tableNumber == 253 || tableNumber == 254 || tableNumber == 255) {
                    Common.logger.error("Trying to add table with same number as reserved value. Skipping.")
                    continue
                }

                if (!IPRoute.tableNames.containsKey(tableNumber)) {
                    IPRoute.tableNames[tableNumber] = tableName
                } else {
                    if (IPRoute.tableNames[tableNumber] != tableName) {
                        Common.logger.error("Trying to add table with the same number as another table. Skipping")
                    }
                }
            }

            val table = StringBuilder(2048)
            for ((tableNumber, tableName) in IPRoute.tableNames) {
                 table.append(tableNumber).append("  ").append(tableName).append("\n");
            }

            val policyRouteFile = File("/etc/iproute2/rt_tables").absoluteFile
            if (!policyRouteFile.canRead()) {
                throw IOException("Unable to initialize policy routing tables. Something is SERIOUSLY wrong, aborting startup!")
            }

            try {
                BufferedWriter(FileWriter(policyRouteFile)).use { writer ->
                    writer.write(reservedTable.toString())
                    writer.write(table.toString())
                    writer.flush()
                }
            } catch (e: IOException) {
                Common.logger.error("Error saving routing table file: {}", policyRouteFile, e)
            }
        } else {
            throw RuntimeException("NOT IMPL.")
        }
    }
}
