/*
 * Ban sao noi dung SQL Employee duoc cung cap kem de.
 * Chay sau 01_create_departments_official.sql.
 */
USE
[MSS301_2026_PE]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[employees]
(
    [
    employee_id] [
    int]
    IDENTITY
(
    1,
    1
) NOT NULL,
    [full_name] [nvarchar]
(
    100
) NOT NULL,
    [email] [nvarchar]
(
    100
) NOT NULL,
    [position] [nvarchar]
(
    30
) NOT NULL,
    [status] [nvarchar]
(
    10
) NOT NULL,
    [start_date] [date] NOT NULL,
    [end_date] [date] NULL,
    [department_id] [int] NOT NULL,
    CONSTRAINT [PK_Employees] PRIMARY KEY CLUSTERED
(
[
    employee_id]
    ASC
)
    )
    GO

ALTER TABLE [dbo].[employees] WITH CHECK ADD CONSTRAINT [CK_Employees_Dates]
    CHECK (([end_date] IS NULL OR [end_date] >= [start_date]))
    GO

ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [CK_Employees_Dates]
    GO

ALTER TABLE [dbo].[employees] WITH CHECK ADD CONSTRAINT [CK_Employees_Status]
    CHECK (([status] = 'LEFT' OR [status] = 'RETIRED' OR [status] = 'ACTIVE' OR [status] = 'INACTIVE'))
    GO

ALTER TABLE [dbo].[employees] CHECK CONSTRAINT [CK_Employees_Status]
    GO
