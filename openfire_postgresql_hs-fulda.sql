DROP TABLE ofUser;

CREATE TABLE ofUser (
  uid						VARCHAR(64)		NOT NULL							,
  password			CHAR(40)			NULL									,
  name					VARCHAR(256)	NULL									,
  email					VARCHAR(256)	NULL									,
  creationDate	BIGINT				NOT NULL							,
  admin					BOOLEAN				NOT NULL DEFAULT TRUE	,
  avatar				TEXT					NULL									,
  avatar_mime		VARCHAR(32)		NULL									,
  								  
  CONSTRAINT ofUser_pk PRIMARY KEY (uid)
);

CREATE INDEX ofUser_cDate_idx ON ofUser (creationDate);
