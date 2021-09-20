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

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.VoltTypeException;
import org.voltdb.types.GeographyPointValue;
import org.voltdb.types.GeographyValue;

import java.math.BigDecimal;


public class InsertOneRow extends VoltProcedure {

    // We don't want to have to change these in more than one place (besides the DDL file)
    final static String COLUMN_NAMES_NO_ID = "MOD_ID, TINY, SMALL, INTEG, BIG, FLOT, DECML, TIMESTMP,"
            + " VCHAR_INLINE,  VCHAR_INLINE_MAX,  VCHAR_OUTLINE_MIN,  VCHAR_OUTLINE,  VCHAR_DEFAULT,"
            + "VARBIN_INLINE, VARBIN_INLINE_MAX, VARBIN_OUTLINE_MIN, VARBIN_OUTLINE, VARBIN_DEFAULT,"
            + "POINT, POLYGON";
    private final static String COLUMN_NAME_LIST = "ID, " + COLUMN_NAMES_NO_ID;
    private final static String VALUES_LIST = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";
    private final static int NUM_PARAMETERS = 21;

    // Declare the SQL Statements to be used
    private final static SQLStmt INSERT_VALUES_DUSB_R1 = new SQLStmt(
            "INSERT INTO DUSB_R1 ( "+COLUMN_NAME_LIST+" ) VALUES ( "+VALUES_LIST+" );");
    final static SQLStmt INSERT_VALUES_DUSB_P1 = new SQLStmt(
            "INSERT INTO DUSB_P1 ( "+COLUMN_NAME_LIST+" ) VALUES ( "+VALUES_LIST+" );");


    // The run() method, as required for each VoltProcedure
    public VoltTable[] run(long id, String tableName,
            String[] columnNames, String[] columnValues)
            throws VoltAbortException
    {
        // Determine which SQLStmt to use
        SQLStmt sqlStatement = getInsertStatement(tableName);

        // Get the query args, as an Object array
        Object[] args = getInsertArgs(id, columnNames, columnValues);

        // Queue the query
        voltQueueSQL(sqlStatement, args);

        // Execute the query
        VoltTable[] vt = voltExecuteSQL(true);

        return vt;
    }


    // Determine which SQLStmt to use, based on tableName
    SQLStmt getInsertStatement(String tableName) {
        SQLStmt sqlStatement = null;
        if (tableName == null) {
            throw new VoltAbortException("Illegal null table name ("+tableName+").");
        } else if ( "DUSB_R1".equals(tableName.toUpperCase()) ) {
            sqlStatement = INSERT_VALUES_DUSB_R1;
        } else if ( "DUSB_P1".equals(tableName.toUpperCase()) ) {
            sqlStatement = INSERT_VALUES_DUSB_P1;
        } else {
            throw new VoltAbortException("Unknown table name: '"+tableName+"'.");
        }
        return sqlStatement;
    }


    // Determine which SQLStmt to use, based on tableName
    Object[] getInsertArgs(long id, String[] columnNames, String[] columnValues) {

        int numColumns = columnNames.length;
        if (numColumns != columnValues.length) {
            throw new VoltAbortException("Different lengths for columnNames ("+numColumns
                    + ") and columnValues ("+columnValues.length+") parameters.");
        }

        Object[] argsArray = new Object[NUM_PARAMETERS];

        // Initialize all column values as null, by default
        for (int i=0; i < numColumns; i++) {
            argsArray[i] = null;
        }

        // A few special cases ...
        // The first two elements get the 'id' value (for columns ID and MOD_ID)
        argsArray[0] = id;
        argsArray[1] = id;

        // Numerical columns whose types have a special null value in Volt
        argsArray[2] = VoltType.TINYINT.getNullValue();
        argsArray[3] = VoltType.SMALLINT.getNullValue();
        argsArray[4] = VoltType.INTEGER.getNullValue();
        argsArray[5] = VoltType.BIGINT.getNullValue();
        argsArray[6] = VoltType.FLOAT.getNullValue();

        // Determine which columns are non-null
        for (int i=0; i < numColumns; i++) {
            try {
                switch (columnNames[i].toUpperCase()) {
                case "TINY":
                case "TINYINT":
                    argsArray[2] = Byte.parseByte(columnValues[i]);
                    break;
                case "SMALL":
                case "SMALLINT":
                    argsArray[3] = Short.parseShort(columnValues[i]);
                    break;
                case "INT":
                case "INTEG":
                case "INTEGER":
                    argsArray[4] = Integer.parseInt(columnValues[i]);
                    break;
                case "BIG":
                case "BIGINT":
                    argsArray[5] = Long.parseLong(columnValues[i]);
                    break;
                case "FLOT":
                case "FLOAT":
                    argsArray[6] = Double.parseDouble(columnValues[i]);
                    break;
                case "DECML":
                case "DECIMAL":
                    argsArray[7] = new BigDecimal(columnValues[i]);
                    break;
                case "TIME":
                case "TIMESTMP":
                case "TIMESTAMP":
                    argsArray[8] = columnValues[i];
                    break;
                case "VCHAR_INLINE":
                    argsArray[9] = columnValues[i];
                    break;
                case "VCHAR_INLINE_MAX":
                    argsArray[10] = columnValues[i];
                    break;
                case "VCHAR_OUTLINE_MIN":
                    argsArray[11] = columnValues[i];
                    break;
                case "VCHAR_OUTLINE":
                    argsArray[12] = columnValues[i];
                    break;
                case "VCHAR_DEFAULT":
                case "VARCHAR":
                    argsArray[13] = columnValues[i];
                    break;
                case "VARBIN_INLINE":
                    argsArray[14] = columnValues[i].getBytes().toString();
                    break;
                case "VARBIN_INLINE_MAX":
                    argsArray[15] = columnValues[i].getBytes().toString();
                    break;
                case "VARBIN_OUTLINE_MIN":
                    argsArray[16] = columnValues[i].getBytes().toString();
                    break;
                case "VARBIN_OUTLINE":
                    argsArray[17] = columnValues[i].getBytes().toString();
                    break;
                case "VARBIN_DEFAULT":
                case "VARBINARY":
                    argsArray[18] = columnValues[i].getBytes().toString();
                    break;
                case "POINT":
                case "GEOGRAPHY_POINT":
                    argsArray[19] = GeographyPointValue.fromWKT(columnValues[i]);
                    break;
                case "POLYGON":
                case "GEOGRAPHY":
                    argsArray[20] = new GeographyValue(columnValues[i]);
                    break;
                default:
                    throw new VoltTypeException("Unknown column name: '"+columnNames[i]+"'.");
                }
            } catch (IllegalArgumentException e) {
                throw new VoltTypeException("Unable to convert value '"+columnValues[i]
                        +"', for column name '"+columnNames[i]+"'.", e);
            } // end of try/catch

        } // end of for loop

        return argsArray;
    }

}
