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

IF (OBJECT_ID(N'Foods', N'U') IS NOT NULL OR OBJECT_ID(N'restaurants', N'U') IS NOT NULL OR OBJECT_ID(N'Category', N'U') IS NOT NULL)
BEGIN
    THROW
50001, 'PEGen refused to overwrite an existing table. Use a fresh database or the official exam SQL.', 1;
END;
GO

CREATE TABLE Category
(
    category_id INT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_Category PRIMARY KEY (category_id),
    CONSTRAINT UQ_Category_name UNIQUE (name)
);
GO

CREATE TABLE restaurants
(
    restaurant_id INT IDENTITY(1,1) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    owner_name    VARCHAR(100) NOT NULL,
    address       VARCHAR(100) NOT NULL,
    open_date     DATETIME2    NOT NULL,
    price_from    INT NULL,
    price_to      INT NULL,
    phone         VARCHAR(11)  NOT NULL,
    status        VARCHAR(10)  NOT NULL,
    category_id   INT          NOT NULL,
    CONSTRAINT PK_restaurants PRIMARY KEY (restaurant_id),
    CONSTRAINT UQ_restaurants_name UNIQUE (name),
    CONSTRAINT CK_restaurants_status CHECK (status IN (N'ACTIVE', N'INACTIVE'))
);
GO

CREATE TABLE Foods
(
    food_id       INT IDENTITY(1,1) NOT NULL,
    name          NVARCHAR(100) NOT NULL,
    price         INT NOT NULL,
    ingredient    NVARCHAR(500) NOT NULL,
    status        NVARCHAR(20) NOT NULL,
    restaurant_id INT NOT NULL,
    CONSTRAINT PK_Foods PRIMARY KEY (food_id),
    CONSTRAINT CK_Foods_status CHECK (status IN (N'ACTIVE', N'INACTIVE'))
);
GO

