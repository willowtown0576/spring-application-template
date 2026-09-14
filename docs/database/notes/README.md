# Database notes

schema変更はFlyway migrationで行う。生成されるtable定義・ER図は `build/documentation/database/` を参照する。

- `feature`: sample featureが所有するschema。
- `system`: Spring Batchが所有するmetadata。column型とsequenceはframework仕様を維持する。
- `public.flyway_schema_history`: application全体の共通migration履歴。tblsの出力対象には含めない。

詳細は [database.md](../../database.md) を参照する。
