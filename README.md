# ReportPlugin

Minecraft サーバー向けの通報プラグイン。GUI を使って通報内容を選択し、Discord Webhook に送信できます。

## 機能
- GUI 形式で通報理由を選択
- 破壊 / 嫌がらせ / その他の通報に対応
- 詳細な内容をチャット入力
- 通報対象プレイヤーをオンライン一覧から選択 or 名前入力
- Webhook へ座標付きで送信
- 通報スパム対策（5分で10件まで）
- 特定プレイヤーを通報禁止に設定可能

## インストール
1. jar を `plugins/` に入れる  
2. 起動して生成された `config.yml` を編集  
3. Webhook URL を設定

## 通報を制限
- report.blockを持っていれば、/reportblock Steve でそのプレイヤーからの通報を制限できます
