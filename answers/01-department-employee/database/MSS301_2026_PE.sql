-- SAFETY: this generated script never drops an existing exam table.
-- Prefer the official SQL files when they were supplied with the question.
-- If any target table already exists, use a fresh database or review it manually.

IF
DB_ID(N'MSS301_2026_PE') IS NULL
BEGIN
    CREATE
DATABASE MSS301_2026_PE;
END
GO

USE MSS301_2026_PE;
GO

IF (OBJECT_ID(N'employees', N'U') IS NOT NULL OR OBJECT_ID(N'departments', N'U') IS NOT NULL)
BEGIN
    THROW
50001, 'PEGen refused to overwrite an existing table. Use a fresh database or the official exam SQL.', 1;
END;
GO

CREATE TABLE departments
(
    department_id  INT IDENTITY(1,1) NOT NULL,
    name           NVARCHAR(50) NOT NULL,
    code           NVARCHAR(10) NOT NULL,
    effective_date DATE NULL,
    status         NVARCHAR(10) NULL,
    location       NVARCHAR(100) NULL,
    parent_id      INT NULL,
    CONSTRAINT PK_departments PRIMARY KEY (department_id),
    CONSTRAINT UQ_departments_code UNIQUE (code),
    CONSTRAINT CK_departments_status CHECK (status IS NULL OR status IN (N'ACTIVE', N'INACTIVE', N'CLOSED')),
    CONSTRAINT CK_departments_effective_date CHECK (effective_date IS NULL OR effective_date >= CAST(GETDATE() AS DATE))
);
GO

CREATE TABLE employees
(
    employee_id   INT IDENTITY(1,1) NOT NULL,
    full_name     NVARCHAR(100) NOT NULL,
    email         NVARCHAR(100) NOT NULL,
    position      NVARCHAR(30) NOT NULL,
    status        NVARCHAR(10) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE NULL,
    department_id INT  NOT NULL,
    CONSTRAINT PK_employees PRIMARY KEY (employee_id),
    CONSTRAINT CK_employees_status CHECK (status IN (N'LEFT', N'RETIRED', N'ACTIVE', N'INACTIVE')),
    CONSTRAINT CK_employees_end_date_order CHECK (end_date IS NULL OR end_date >= start_date)
);
GO
