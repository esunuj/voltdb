-- DDL for the delete-update-snapshot-benchmark.  Note that 'DUSB'
-- is short for 'delete-update-snapshot-benchmark'.

file -inlinebatch END_OF_BATCH1

DROP PROCEDURE UpdateOneRowP IF EXISTS;
DROP PROCEDURE DeleteOneRowP IF EXISTS;
DROP PROCEDURE InsertOneRowP IF EXISTS;
DROP PROCEDURE UpdateOneRow  IF EXISTS;
DROP PROCEDURE DeleteOneRow  IF EXISTS;
DROP PROCEDURE InsertOneRow  IF EXISTS;
DROP PROCEDURE UpdateRows    IF EXISTS;
DROP PROCEDURE DeleteRows    IF EXISTS;
DROP PROCEDURE BulkInsert    IF EXISTS;

DROP TABLE DUSB_P1 IF EXISTS CASCADE;
DROP TABLE DUSB_R1 IF EXISTS CASCADE;

END_OF_BATCH1

-- Load all the classes from the jar, including the procedures that will
-- also need to be 'CREATE-ed' below
LOAD CLASSES dusbench.jar;

file -inlinebatch END_OF_BATCH2

CREATE TABLE DUSB_R1 (
  ID                 BIGINT NOT NULL PRIMARY KEY,
  MOD_ID             BIGINT,
  TINY               TINYINT,
  SMALL              SMALLINT,
  INTEG              INTEGER,
  BIG                BIGINT,
  FLOT               FLOAT,
  DECML              DECIMAL,
  TIMESTMP           TIMESTAMP,
  VCHAR_INLINE       VARCHAR(14),
  VCHAR_INLINE_MAX   VARCHAR(63 BYTES),
  VCHAR_OUTLINE_MIN  VARCHAR(64 BYTES),
  VCHAR_OUTLINE      VARCHAR(20),
  VCHAR_DEFAULT      VARCHAR,
  VARBIN_INLINE      VARBINARY(32),
  VARBIN_INLINE_MAX  VARBINARY(63),
  VARBIN_OUTLINE_MIN VARBINARY(64),
  VARBIN_OUTLINE     VARBINARY(128),
  VARBIN_DEFAULT     VARBINARY,
  POINT              GEOGRAPHY_POINT,
  POLYGON            GEOGRAPHY,  -- TODO: GEOGRAPHY(size)
);

CREATE TABLE DUSB_P1 (
  ID                 BIGINT NOT NULL PRIMARY KEY,
  MOD_ID             BIGINT,
  TINY               TINYINT,
  SMALL              SMALLINT,
  INTEG              INTEGER,
  BIG                BIGINT,
  FLOT               FLOAT,
  DECML              DECIMAL,
  TIMESTMP           TIMESTAMP,
  VCHAR_INLINE       VARCHAR(14),
  VCHAR_INLINE_MAX   VARCHAR(63 BYTES),
  VCHAR_OUTLINE_MIN  VARCHAR(64 BYTES),
  VCHAR_OUTLINE      VARCHAR(20),
  VCHAR_DEFAULT      VARCHAR,
  VARBIN_INLINE      VARBINARY(32),
  VARBIN_INLINE_MAX  VARBINARY(63),
  VARBIN_OUTLINE_MIN VARBINARY(64),
  VARBIN_OUTLINE     VARBINARY(128),
  VARBIN_DEFAULT     VARBINARY,
  POINT              GEOGRAPHY_POINT,
  POLYGON            GEOGRAPHY,  -- TODO: GEOGRAPHY(size)
);
PARTITION TABLE DUSB_P1 ON COLUMN ID;

CREATE PROCEDURE FROM CLASS procedures.BulkInsert;
CREATE PROCEDURE FROM CLASS procedures.DeleteRows;
CREATE PROCEDURE FROM CLASS procedures.UpdateRows;

CREATE PROCEDURE FROM CLASS procedures.InsertOneRow;
CREATE PROCEDURE FROM CLASS procedures.DeleteOneRow;
CREATE PROCEDURE FROM CLASS procedures.UpdateOneRow;

CREATE PROCEDURE PARTITION ON TABLE DUSB_P1 COLUMN ID
                 FROM CLASS procedures.InsertOneRowP;
CREATE PROCEDURE PARTITION ON TABLE DUSB_P1 COLUMN ID
                 FROM CLASS procedures.DeleteOneRowP;
CREATE PROCEDURE PARTITION ON TABLE DUSB_P1 COLUMN ID
                 FROM CLASS procedures.UpdateOneRowP;

END_OF_BATCH2
