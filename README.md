migration.keptos3epub
=======================

1. 利用pentaho kettle DI，執行轉換檔案ExportNonTransKEP.ktr，輸出資料表EBookNotInKGL表示尚未出現在KGL的書籍資料。
2. 執行本程式，需搭配kep-to-epub java程式。
3. 程式將kep轉換成epub後，會直接上傳至s3指定的路徑下，並且輸出轉換結果資料表EpubConvertResult。

---
檔案上傳完成後，下一步要執行書籍資料metadata轉換，請執行轉換檔案ImportBookMetadataToKGL.ktr及ImportBookDetailMetadataToKGL.ktr。
