/*
  OPTIONAL TEST DATA ONLY
  Run after MSS301_2026_PE.sql when Category is empty and you need IDs for
  POST /api/restaurants. It is idempotent and does not delete existing data.
*/
USE
[MSS301_2026_PE];
GO

SET XACT_ABORT ON;
BEGIN
TRANSACTION;

IF
OBJECT_ID(N'[dbo].[Category]', N'U') IS NULL
BEGIN
    THROW
50002, 'Table dbo.Category does not exist. Run the schema script first.', 1;
END;

IF
NOT EXISTS (SELECT 1 FROM [dbo].[Category] WHERE [name] = N'Vietnamese Food')
    INSERT INTO [dbo].[Category] ([name]) VALUES (N'Vietnamese Food');

IF
NOT EXISTS (SELECT 1 FROM [dbo].[Category] WHERE [name] = N'Barbecue')
    INSERT INTO [dbo].[Category] ([name]) VALUES (N'Barbecue');

IF
NOT EXISTS (SELECT 1 FROM [dbo].[Category] WHERE [name] = N'Fast Food')
    INSERT INTO [dbo].[Category] ([name]) VALUES (N'Fast Food');

COMMIT TRANSACTION;
GO

SELECT [category_id], [name]
FROM [dbo].[Category]
ORDER BY [category_id];
GO
