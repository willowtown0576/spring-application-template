package dev.template.application.web.rest;

import dev.template.application.feature.api.command.FeatureCommandFailure;
import dev.template.application.feature.api.model.FeatureStatus;
import dev.template.application.feature.api.query.FeatureSearchCursor;
import dev.template.application.feature.api.query.FeatureSortField;
import dev.template.application.feature.api.query.SearchFeaturesParam;
import dev.template.application.web.FeatureSearchCursorCodec;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** FeatureのHTTP契約へ、項目の意味と相互に対応する架空の利用例を付与する。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class FeatureOpenApiConfiguration {
    /** 大阪支店の契約を示す架空のUUID v7。 */
    private static final String OSAKA_ID = "01a1a49b-3ec0-73f8-b98c-c44a15d41e10";
    /** 大阪支店の更新時刻。 */
    private static final String OSAKA_TIME = "2026-11-04T03:30:00Z";
    /** 再送時にも維持する架空の操作キー。 */
    private static final String REQUEST_ID = "2bf22be6-02bd-46e8-a4be-b8f2170387f5";
    /** 東京本社の契約を示す架空のUUID v7。 */
    private static final String TOKYO_ID = "01a1aaef-ba80-7a31-8e51-89d16a1928db";
    /** 東京本社の更新時刻。 */
    private static final String TOKYO_TIME = "2026-11-05T09:00:00Z";

    /**
     * 登録入力と、同じキーで再送する二行の例を記載する。
     * @param api 生成中の資料
     */
    private static void describeCreation(final OpenAPI api) {
        final Operation create = api.getPaths().get("/api/v1/features").getPost();
        describeWrite(create, "Featureを1件登録", """
            feature:writeが必要。名前を加工せず保存し、同名も登録できます。status省略時はACTIVEです。
            登録時はversion=1、登録日時と更新日時は同じサーバー時刻です。201のLocationからGETで参照できます。
            nameの欠落・nullは400、文字数や空白等の業務制約違反は422です。
            単件登録には再送の重複抑止がありません。再送を区別する場合は操作キー付きの複数登録を利用してください。
            """);
        requestExample(create, "tokyoContract", "東京本社の設備保守契約を有効状態で登録",
            Map.of("name", "東京本社の設備保守契約", "status", "ACTIVE"));
        responseExample(create, "201", "created", "作成ID。実際の操作では返されたIDを保存する", Map.of("id", TOKYO_ID));
        create.getResponses().get("201").getHeaders().get("Location")
            .description("作成したFeatureの参照URL。GETで現在のversionを取得できます。").getSchema()
            .setExample("/api/v1/features/" + TOKYO_ID);
        responseExample(create, "422", "invalidName", "空白だけの名前を拒否。保存されない",
            problem(422, "Unprocessable Content", "/api/v1/features", 1, "name", "INVALID_NAME"));

        final Operation bulk = api.getPaths().get("/api/v1/features/bulk").getPost();
        describeWrite(bulk, "Featureを複数登録", """
            feature:writeが必要。rowsは1〜100行。全行を一つのtransactionで登録し、一行でも不正なら全件を保存しません。
            各行のnameとstatusは必須です。結果のID配列は入力順で、201やLocationではなく200を返します。
            requestIdは操作ごとのUUIDです。同じ認証主体・requestId・入力順を含む同一内容の再送は、同じID配列を返します。
            同じキーで内容や行順を変えると409 REPLAY_MISMATCHです。別の登録には新しいキーを発行してください。
            キーの自動失効はありません。認証主体はsessionから取得するためbodyへ指定しません。
            """);
        requestExample(bulk, "twoContracts", "東京・大阪の設備保守契約を同時登録", Map.of("requestId", REQUEST_ID, "rows", List
            .of(Map.of("name", "東京本社の設備保守契約", "status", "ACTIVE"), Map.of("name", "大阪支店の設備保守契約", "status", "ACTIVE"))));
        responseExample(bulk, "200", "createdInInputOrder", "rows[0]が東京、rows[1]が大阪のID", List.of(TOKYO_ID, OSAKA_ID));
        responseExample(bulk, "409", "changedReplay", "保存済みのキーで内容を変えた。追加登録されない",
            problem(409, "Conflict", "/api/v1/features/bulk", 0, "requestId", "REPLAY_MISMATCH"));
        responseExample(bulk, "422", "secondRowInvalid", "2行目のnameが不正。1行目も保存されない",
            problem(422, "Unprocessable Content", "/api/v1/features/bulk", 2, "name", "INVALID_NAME"));
    }

    /**
     * 履歴退避を伴う単件・複数削除と、競合時の拒否を記載する。
     * @param api 生成中の資料
     */
    private static void describeDeletion(final OpenAPI api) {
        final Operation single = api.getPaths().get("/api/v1/features/{id}").getDelete();
        describeWrite(single, "Featureを履歴へ退避して1件削除", """
            feature:writeが必要。versionは確認直前のGETまたは検索で取得した値をquery parameterへ指定します。
            削除直前の全カラムをfeature.feature_historyへ保存し、削除日時と認証主体を付加して元のtableから取り除きます。
            履歴保存と削除は一つのtransactionです。versionや更新日時は削除で増加・変更しません。
            未検出は404、競合は409、versionが1未満なら422。欠落や形式不正は400。競合時は最新内容を確認して削除を再判断してください。
            成功は200と削除ID。以降の通常検索・GETには現れません。同じ削除の再送は404で履歴を増やしません。
            履歴参照・復元APIはありません。登録再送の台帳は維持し、削除済みの登録操作を再送しても復活しません。
            """);
        parameter(single, "id", "削除を確認した対象ID。", TOKYO_ID);
        parameter(single, "version", "同じ対象のGETまたは検索で読んだversion。削除時も競合を検査します。", 1).getSchema()
            .setMinimum(BigDecimal.ONE);
        responseExample(single, "200", "archivedAndDeleted", "東京本社の契約を履歴へ保存し、通常tableから削除", TOKYO_ID);
        responseExample(single, "404", "missing", "削除済みまたは存在しない対象",
            problem(404, "Not Found", "/api/v1/features/" + TOKYO_ID, 1, "id", "NOT_FOUND"));
        responseExample(single, "409", "stale", "確認後に更新されたため履歴保存も削除も行わない",
            problem(409, "Conflict", "/api/v1/features/" + TOKYO_ID, 1, "version", "CONFLICT"));
        responseExample(single, "422", "invalidVersion", "versionが1未満",
            problem(422, "Unprocessable Content", "/api/v1/features/" + TOKYO_ID, 1, "version", "INVALID_VERSION"));
        final Operation bulk = api.getPaths().get("/api/v1/features/bulk-delete").getPost();
        describeWrite(bulk, "Featureを履歴へ退避して複数削除", """
            feature:writeが必要。targetsは1〜100件で各行のidとversionが必須。同じIDの重複は422です。
            DELETE bodyのclient互換性へ依存せず、POSTで削除対象の集合を指定します。実行主体は認証から取得しbodyでは指定しません。
            全対象をlockし、全件の存在とversionを確認してから、削除前の全カラムと共通の削除日時・実行主体を履歴へ保存して削除します。
            一行でも不正・未検出・競合なら全行を拒否し、技術障害時も元データと履歴を全件rollbackします。
            入力不正は422。入力が正しい場合に存在とversionを全件検査し、競合があれば409、未検出のみなら404です。
            成功は200と入力順のID配列。履歴は自動削除・上書きをせず、画面やAPIからの復元機能はありません。
            """);
        requestExample(bulk, "twoTargets", "東京・大阪の契約を確認時のversionで同時に削除",
            Map.of("targets", List.of(Map.of("id", TOKYO_ID, "version", 1), Map.of("id", OSAKA_ID, "version", 1))));
        responseExample(bulk, "200", "bothArchived", "全件を履歴へ保存して削除した入力順のID", List.of(TOKYO_ID, OSAKA_ID));
        responseExample(bulk, "404", "secondMissing", "2行目が存在しない。1行目も削除しない",
            problem(404, "Not Found", "/api/v1/features/bulk-delete", 2, "id", "NOT_FOUND"));
        responseExample(bulk, "409", "secondStale", "2行目が競合。1行目も削除しない",
            problem(409, "Conflict", "/api/v1/features/bulk-delete", 2, "version", "CONFLICT"));
        responseExample(bulk, "422", "duplicate", "2行目のIDが1行目と重複。全件を拒否",
            problem(422, "Unprocessable Content", "/api/v1/features/bulk-delete", 2, "id", "DUPLICATE_ID"));
        final Schema<?> request = api.getComponents().getSchemas().get("DeleteFeaturesRequest");
        request.setRequired(List.of("targets"));
        property(request, "targets", "確認済みのIDとversion。全件成功または全件拒否。", null).minItems(1).maxItems(100);
        final Schema<?> target = api.getComponents().getSchemas().get("FeatureDeletionTarget");
        target.setRequired(List.of("id", "version"));
        property(target, "id", "削除対象のID。実際の登録・検索応答から取得します。", TOKYO_ID);
        property(target, "version", "確認時のversion。現在値と異なれば全件を拒否します。", 1).minimum(BigDecimal.ONE);
    }

    /**
     * 項目の必須条件・精度・意味を、HTTPに依存しない業務型のschemaにも補完する。
     * @param api 生成中の資料
     */
    private static void describeSchemas(final OpenAPI api) {
        for (final String name : List.of("CreateFeatureRequest", "CreateFeatureParam", "UpdateFeatureRequest",
            "UpdateFeatureParam")) {
            final Schema<?> schema = api.getComponents().getSchemas().get(name);
            property(schema, "name", "名前。1〜100 Unicode code point。前後空白も保存。同名可。空文字、半角空白のみ、NUL、不正surrogateは422。",
                "東京本社の設備保守契約").minLength(1).maxLength(100);
            property(schema, "status", "ACTIVE=有効、INACTIVE=無効。無効も参照・更新できます。単件登録のみ省略時ACTIVE、他操作は必須。", "ACTIVE");
            schema.setRequired(name.equals("CreateFeatureRequest") ? List.of("name") : List.of("name", "status"));
            if (name.startsWith("Update")) {
                property(schema, "version", "直前のGETまたは検索で読んだversion。初期値1。現在値と不一致なら409。", 1).minimum(BigDecimal.ONE);
                schema.addRequiredItem("version");
            }
            if (name.equals("UpdateFeatureParam")) {
                property(schema, "id", "更新対象のID。登録応答または検索結果から取得します。", TOKYO_ID);
                schema.addRequiredItem("id");
            }
        }
        final Schema<?> created = api.getComponents().getSchemas().get("CreateFeatureResponse");
        property(created, "id", "サーバーが発行するUUID v7。画面の表示用連番ではありません。", TOKYO_ID);
        created.setRequired(List.of("id"));
        final Schema<?> bulk = api.getComponents().getSchemas().get("CreateFeaturesRequest");
        bulk.setDescription("同一操作の再送を区別できる複数登録入力。");
        bulk.setRequired(List.of("requestId", "rows"));
        property(bulk, "requestId", "操作ごとに発行し、同じ内容の再送には同じ値を使用するUUID。", REQUEST_ID);
        property(bulk, "rows", "入力順の登録行。1〜100行。全件成功または全件拒否。", null).minItems(1).maxItems(100);
        final Schema<?> details = api.getComponents().getSchemas().get("FeatureDetails");
        details.setDescription("1件の現在の保存内容。ID・versionは後続の更新に使用します。");
        details.setRequired(List.of("id", "name", "status", "updatedAt", "version"));
        property(details, "id", "サーバー発行の内部ID。更新pathまたは一括更新行に使用します。", TOKYO_ID);
        property(details, "name", "保存された名前。", "東京本社の設備保守契約").minLength(1).maxLength(100);
        property(details, "status", "ACTIVE=有効、INACTIVE=無効。", "ACTIVE");
        property(details, "updatedAt", "最後に値を変更したサーバー時刻。UTCのISO 8601、microsecond精度。登録時も設定。登録日時は公開しません。", TOKYO_TIME);
        property(details, "version", "初期値1。名前または状態に実変更がある行だけ1増加。変更なしでは日時も維持。", 1).minimum(BigDecimal.ONE);
        final Schema<?> page = api.getComponents().getSchemas().get("SearchFeaturesResponse");
        page.setRequired(List.of("items", "previous", "next"));
        property(page, "items", "指定順の参照行。最大size件。該当なしは空配列。総件数は返しません。", null).maxItems(100);
        for (final String direction : List.of("previous", "next")) {
            property(page, direction,
                direction.equals("previous") ? "前ページ境界。前ページがない場合はnull。" : "次ページ境界。次ページがない場合はnull。", null)
                .setTypes(Set.of("string", "null"));
        }
        final Schema<?> problem = api.getComponents().getSchemas().get("ProblemDetail");
        problem.setDescription("エラー応答。業務拒否はトップレベルのissuesを持ち、技術障害や認証エラーにはissuesがありません。");
        final var reason = new StringSchema();
        reason.setEnum(Arrays.stream(FeatureCommandFailure.Reason.values()).map(Enum::name).toList());
        reason.setDescription(
            "INVALID_NAME/STATUS/VERSION/REQUEST=入力不正、DUPLICATE_ID=対象重複、NOT_FOUND=未検出、CONFLICT=version不一致、REPLAY_MISMATCH=再送内容不一致。");
        final var issue = new ObjectSchema();
        issue.addProperty("row",
            new IntegerSchema().minimum(BigDecimal.ZERO).description("1始まりの入力行。単件は1、操作全体は0。").example(2));
        issue.addProperty("field", new StringSchema()
            .description("問題の項目名。name、status、version、id、rows、requestId、targets、actor。").example("version"));
        issue.addProperty("reason", reason);
        issue.setRequired(List.of("row", "field", "reason"));
        api.getComponents().addSchemas("FeatureCommandIssue", issue);
        problem.addProperty("issues",
            new ArraySchema().items(new Schema<>().$ref("#/components/schemas/FeatureCommandIssue"))
                .description("Featureの業務拒否の一覧。入力不正は422。入力が正しい場合に存在・versionを検査し、409を404より優先します。"));
        final var updates = api.getPaths().get("/api/v1/features/bulk").getPut().getRequestBody().getContent()
            .get("application/json").getSchema();
        updates.setMinItems(1);
        updates.setMaxItems(100);
    }

    /**
     * 検索条件一式、実codecの境界、空結果を記載する。
     * @param api 生成中の資料
     * @param cursors cursor変換
     */
    private static void describeSearch(final OpenAPI api, final FeatureSearchCursorCodec cursors) {
        final Operation find = api.getPaths().get("/api/v1/features/{id}").getGet();
        find.summary("IDでFeatureの現在値を取得").description(
            "feature:readが必要。更新前にid・name・status・versionを取得します。存在しないIDは404です。例のIDは架空です。実際は登録・検索の応答から取得してください。");
        parameter(find, "id", "登録または検索で取得したUUID。", TOKYO_ID);
        responseExample(find, "200", "tokyoContract", "登録直後の東京本社の契約。更新にはversion=1を使う",
            details(TOKYO_ID, "東京本社の設備保守契約", TOKYO_TIME));
        responseExample(find, "404", "notFound", "対象が存在しない", Map.of("type", "about:blank", "title", "Not Found",
            "status", 404, "detail", "Feature not found.", "instance", "/api/v1/features/" + TOKYO_ID));
        final Operation search = api.getPaths().get("/api/v1/features").getGet();
        search.summary("条件検索とカーソルでの前後移動").description("""
            feature:readが必要。条件はAND。名前の部分一致と任意の状態で絞り込み、名前または更新日時の一項目で並べます。
            既定は更新日時降順、20件。名前順はDBの照合順で、かな順ではありません。同値は非表示IDで順序を確定します。
            初回はcursorを省略。nextを同じ条件のcursorへ渡すと次へ、previousをcursorへ渡しprevious=trueで前へ移動します。
            name/status/sort/descending/sizeを変えるときはcursorとpreviousを外してください。条件不一致や不正tokenは400です。
            例: GET /api/v1/features?name=設備&status=ACTIVE&sort=UPDATED_AT&descending=true&size=2
            次の要求は上記の条件を維持して、応答のnextをcursorとして追加します。tokenは解釈・組み立てせず実応答からそのまま使用します。
            COUNT・OFFSET・ページ番号・総ページ数はありません。閲覧中の更新で行の位置が変わるため、snapshotは保証しません。
            """);
        parameter(search, "name", "前後の空白を除去し、大小文字を区別しない部分一致。全半角は区別。%と_は通常文字。省略・空は全件。最大100 code point。", "設備")
            .getSchema().setMaxLength(100);
        parameter(search, "status", "ACTIVE=有効、INACTIVE=無効。省略は両方。", "ACTIVE");
        parameter(search, "sort", "NAMEまたはUPDATED_ATの一項目。二項目の同時指定は不可。", "UPDATED_AT");
        parameter(search, "descending", "true=降順、false=昇順。", true);
        final Schema<?> size = parameter(search, "size", "1〜100件。既定20。例はページ移動が分かるよう2件。", 2).getSchema();
        size.setMinimum(BigDecimal.ONE);
        size.setMaximum(BigDecimal.valueOf(100));
        final var conditions = new SearchFeaturesParam("設備", FeatureStatus.ACTIVE, FeatureSortField.UPDATED_AT, true, 2,
            null, false);
        final String next = cursors.encode(
            new FeatureSearchCursor(UUID.fromString(OSAKA_ID), "大阪支店の設備保守契約", Instant.parse(OSAKA_TIME)), conditions);
        parameter(search, "cursor", "初回は省略。実応答のnextまたはpreviousをそのまま渡す境界token。例は上記5条件に対応。", next).getSchema()
            .setMaxLength(2048);
        parameter(search, "previous", "次へはfalse、前へはtrue。trueの場合cursorが必須。", false);
        final Map<String, Object> page = new LinkedHashMap<>();
        page.put("items",
            List.of(details(TOKYO_ID, "東京本社の設備保守契約", TOKYO_TIME), details(OSAKA_ID, "大阪支店の設備保守契約", OSAKA_TIME)));
        page.put("previous", null);
        page.put("next", next);
        responseExample(search, "200", "firstPage", "有効な設備保守契約の先頭2件。続きがある場合の境界", page);
        final Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("items", List.of());
        empty.put("previous", null);
        empty.put("next", null);
        responseExample(search, "200", "noMatches", "検索条件に一致しない場合", empty);
        responseExample(search, "400", "invalidCursor", "条件変更時に古いcursorを渡した場合など",
            Map.of("type", "about:blank", "title", "Bad Request", "status", 400, "detail",
                "Invalid search cursor or conditions.", "instance", "/api/v1/features"));
    }

    /**
     * 単件と複数の更新例、競合と全件拒否を記載する。
     * @param api 生成中の資料
     */
    private static void describeUpdates(final OpenAPI api) {
        final Operation update = api.getPaths().get("/api/v1/features/{id}").getPut();
        describeWrite(update, "Featureを1件更新", """
            feature:writeが必要。name/status/versionはすべて必須。部分更新ではなく名前と状態の両方を送ります。
            versionは直前のGETまたは検索で取得した値です。名前または状態が変わるとversionが1増加し更新日時が変わります。
            値が変わらない場合も200と対象IDを返しますが、version・日時は維持します。登録日時は変更しません。
            409の場合は最新値を取得し、自分の入力と比較して利用者が再編集した後、確認したversionで再送します。自動上書きはしません。
            """);
        parameter(update, "id", "更新する契約のUUID。bodyのversionはこのIDの読取値を使用します。", TOKYO_ID);
        requestExample(update, "renameTokyoContract", "東京本社の契約名を改訂し無効化。読取時のversion=1を送る",
            Map.of("name", "東京本社の設備保守契約（改訂）", "status", "INACTIVE", "version", 1));
        responseExample(update, "200", "updated", "対象ID。変更後のversionはGETで再取得する", TOKYO_ID);
        responseExample(update, "409", "staleVersion", "参照後に他の処理が更新した。保存されない",
            problem(409, "Conflict", "/api/v1/features/" + TOKYO_ID, 1, "version", "CONFLICT"));
        responseExample(update, "404", "notFound", "更新対象が存在しない",
            problem(404, "Not Found", "/api/v1/features/" + TOKYO_ID, 1, "id", "NOT_FOUND"));
        responseExample(update, "422", "invalidVersion", "versionが1未満または未指定",
            problem(422, "Unprocessable Content", "/api/v1/features/" + TOKYO_ID, 1, "version", "INVALID_VERSION"));
        final Operation bulk = api.getPaths().get("/api/v1/features/bulk").getPut();
        describeWrite(bulk, "Featureを複数更新", """
            feature:writeが必要。bodyは1〜100行の配列で、各行のid/name/status/versionが必須です。各行へ異なる値を指定できます。
            同一IDの重複は422。一行でも不正・未検出・version競合なら全件を保存しません。
            成功は200と実際に値が変わったIDだけの配列を入力順で返します。全行変更なしなら空配列です。
            変更行の更新日時は同じ操作内で共通、versionは各行で1増加します。競合後は全対象の最新値を確認し、明示的に再編集してください。
            入力項目が不正なら存在・versionの検査へ進まず422を返します。入力が正しい場合に存在・versionを全行検査します。
            この検査で競合と未検出が混在する場合は409を優先します。issuesは検出した全行の問題を返します。
            """);
        requestExample(bulk, "twoDifferentUpdates", "東京の名称改訂と大阪の無効化を一回で保存",
            List.of(Map.of("id", TOKYO_ID, "name", "東京本社の設備保守契約（改訂）", "status", "ACTIVE", "version", 1),
                Map.of("id", OSAKA_ID, "name", "大阪支店の設備保守契約", "status", "INACTIVE", "version", 1)));
        responseExample(bulk, "200", "bothChanged", "入力した二行が変わった場合", List.of(TOKYO_ID, OSAKA_ID));
        responseExample(bulk, "200", "unchanged", "期待versionは一致し、全行の値が既存と同じ場合", List.of());
        responseExample(bulk, "409", "secondRowStale", "2行目が競合。1行目も保存されない",
            problem(409, "Conflict", "/api/v1/features/bulk", 2, "version", "CONFLICT"));
        responseExample(bulk, "404", "secondRowMissing", "2行目が未検出。1行目も保存されない",
            problem(404, "Not Found", "/api/v1/features/bulk", 2, "id", "NOT_FOUND"));
        responseExample(bulk, "422", "duplicateTarget", "2行目のIDが1行目と重複。全行を拒否",
            problem(422, "Unprocessable Content", "/api/v1/features/bulk", 2, "id", "DUPLICATE_ID"));
    }

    /**
     * 書き込み操作の説明と、同時に必要なsession・CSRFを記載する。
     * @param operation 対象操作
     * @param summary 概要
     * @param description 利用契約
     */
    private static void describeWrite(final Operation operation, final String summary, final String description) {
        operation.summary(summary).description(description).security(
            List.of(new SecurityRequirement().addList("SessionCookie").addList("CsrfCookie").addList("CsrfHeader")));
    }

    /**
     * 一件の架空の保存内容を作る。
     * @param id 契約ID
     * @param name 契約名
     * @param time 更新時刻
     * @return 初期versionの参照例
     */
    private static Map<String, Object> details(final String id, final String name, final String time) {
        return Map.of("id", id, "name", name, "status", "ACTIVE", "updatedAt", time, "version", 1);
    }

    /**
     * 既存parameterへ意味と具体値を付ける。
     * @param operation 対象操作
     * @param name parameter名
     * @param description 項目の意味
     * @param example 具体例
     * @return 補完対象
     */
    private static Parameter parameter(final Operation operation, final String name, final String description,
        final Object example) {
        final Parameter parameter = operation.getParameters().stream().filter(value -> value.getName().equals(name))
            .findFirst().orElseThrow();
        return parameter.description(description).example(example);
    }

    /**
     * 行番号付き業務拒否を例示する。
     * @param status HTTP status
     * @param title status名
     * @param path request path
     * @param row 行番号
     * @param field 項目名
     * @param reason 型付き理由
     * @return 実応答と同じ階層の例
     */
    private static Map<String, Object> problem(final int status, final String title, final String path, final int row,
        final String field, final String reason) {
        return Map.of("type", "about:blank", "title", title, "status", status, "detail",
            "Feature operation was rejected.", "instance", path, "issues",
            List.of(Map.of("row", row, "field", field, "reason", reason)));
    }

    /**
     * 抽出済みschemaの項目へ意味と例を補う。
     * @param schema 親schema
     * @param name 項目名
     * @param description 項目の意味
     * @param example 具体例。nullなら項目例を設定しない
     * @return 補完対象
     */
    private static Schema<?> property(final Schema<?> schema, final String name, final String description,
        final Object example) {
        final Schema<?> property = schema.getProperties().get(name);
        property.setDescription(description);
        if (example != null) {
            property.setExample(example);
        }
        return property;
    }

    /**
     * request bodyへ名前付きの利用例を付ける。
     * @param operation 対象操作
     * @param name 例の識別名
     * @param summary 例の前提
     * @param value JSON相当の値
     */
    private static void requestExample(final Operation operation, final String name, final String summary,
        final Object value) {
        operation.getRequestBody().getContent().get("application/json").addExamples(name,
            new Example().summary(summary).value(value));
    }

    /**
     * statusに対応するbodyへ具体例を付ける。
     * @param operation 対象操作
     * @param status 応答status
     * @param name 例の識別名
     * @param summary 例の前提
     * @param value JSON相当の値
     */
    private static void responseExample(final Operation operation, final String status, final String name,
        final String summary, final Object value) {
        operation.getResponses().get(status).getContent()
            .get(status.startsWith("2") ? "application/json" : "application/problem+json")
            .addExamples(name, new Example().summary(summary).value(value));
    }

    /**
     * Featureの各操作とschemaへ具体的な利用契約を付与する。
     * @param cursors 本番と共用するcursor変換
     * @return Feature固有の資料補完
     */
    @Bean
    OpenApiCustomizer featureApiDocumentation(final FeatureSearchCursorCodec cursors) {
        return api -> {
            describeSchemas(api);
            describeCreation(api);
            describeDeletion(api);
            describeUpdates(api);
            describeSearch(api, cursors);
        };
    }
}
