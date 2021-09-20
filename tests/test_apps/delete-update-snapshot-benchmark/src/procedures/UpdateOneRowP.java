/* This file is part of VoltDB.
 * Copyright (C) 2021 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package procedures;

import client.benchmark.DUSBenchmark;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;


/** Partitioned version of UpdateOneRow */
public class UpdateOneRowP extends UpdateOneRow {


    // The run() method, as required for each VoltProcedure
    public VoltTable[] run(long idValue, String tableName, String inlineOrOutline)
            throws VoltAbortException
    {
        // Check for a non-partitioned table, which is not allowed here
        if (tableName == null || !DUSBenchmark.PARTITIONED_TABLES.contains(tableName.toUpperCase())) {
            throw new VoltAbortException("Illegal table name ("+tableName+") for UpdateOneRowP.");
        }

        // Determine which SQLStmt to use
        SQLStmt sqlStatement = getUpdateStatement(tableName, inlineOrOutline);

        // Queue the query
        voltQueueSQL(sqlStatement, idValue);

        // Execute the query
        return voltExecuteSQL(true);
    }

}
