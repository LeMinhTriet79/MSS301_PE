/* Table: Category
   Target: MS SQL Server 2016
*/

CREATE TABLE [dbo].[Category]
(
    [
    category_id]
    INT
    IDENTITY
(
    1,
    1
) NOT NULL, -- Auto increment
    [name] NVARCHAR
(
    100
) NOT NULL, -- Unique

-- Khóa chính
    CONSTRAINT [PK_Category] PRIMARY KEY CLUSTERED
(
[
    category_id]
    ASC
),

    -- Ràng buộc duy nhất cho cột Name
    CONSTRAINT [UQ_Category_Name] UNIQUE NONCLUSTERED
(
[
    name]
    ASC
)
    );
GO
