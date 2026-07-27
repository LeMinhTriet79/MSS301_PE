/*
 * Ban sao noi dung SQL Department duoc cung cap kem de.
 * Chay sau khi database MSS301_2026_PE da ton tai.
 */
USE
[MSS301_2026_PE]
GO

CREATE TABLE [dbo].[departments]
(
    [
    department_id]
    INT
    IDENTITY
(
    1,
    1
) NOT NULL,
    [name] NVARCHAR
(
    50
) NOT NULL,
    [code] NVARCHAR
(
    10
) NOT NULL,
    [effective_date] DATE NULL,
    [status] NVARCHAR
(
    10
) NULL,
    [location] NVARCHAR
(
    100
) NULL,
    [parent_id] INT NULL,
    CONSTRAINT [PK_Departments]
    PRIMARY KEY CLUSTERED
(
[
    department_id]
    ASC
),
    CONSTRAINT [UQ_Departments_Code]
    UNIQUE NONCLUSTERED
(
[
    code]
    ASC
),
    CONSTRAINT [CK_Departments_Status]
    CHECK
(
    [
    status]
    IN
(
    'ACTIVE',
    'INACTIVE',
    'CLOSED'
)),
    CONSTRAINT [CK_Departments_EffectiveDate]
    CHECK
(
    [
    effective_date]
    >=
    CAST (
    GETDATE
(
) AS DATE))
    );
GO
