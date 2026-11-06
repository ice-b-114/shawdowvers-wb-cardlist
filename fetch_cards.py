"""拉取影之诗：超凡世界官方卡表（chs / cht），合并分页后输出 JSON。

用法:
    python fetch_cards.py                      # 默认拉取 chs、cht，输出到 data/
    python fetch_cards.py --langs cht --out out
仅依赖标准库。
"""
import argparse
import json
import sys
import time
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

BASE_URL = "https://shadowverse-wb.com/web/CardList/cardList"
USER_AGENT = "Mozilla/5.0 (compatible; svwb-card-fetcher/1.0)"
DICT_KEYS = ("cards", "card_details", "tribe_names", "card_set_names",
             "skill_names", "skill_replace_text_names")


OPENER = urllib.request.build_opener()


def fetch_page(lang: str, offset: int, retries: int = 4) -> dict:
    query = urllib.parse.urlencode({"offset": offset, "include_token": 1})
    req = urllib.request.Request(
        f"{BASE_URL}?{query}",
        headers={"lang": lang, "User-Agent": USER_AGENT, "Accept": "application/json"},
    )
    last_err = None
    for attempt in range(retries):
        try:
            with OPENER.open(req, timeout=30) as resp:
                body = json.loads(resp.read().decode("utf-8"))
            code = body.get("data_headers", {}).get("result_code")
            if code != 1 or "data" not in body:
                raise RuntimeError(f"接口返回异常 result_code={code}（可能在维护）")
            return body["data"]
        except Exception as e:  # noqa: BLE001
            last_err = e
            time.sleep(1.5 * (attempt + 1))
    raise RuntimeError(f"lang={lang} offset={offset} 拉取失败: {last_err}")


def merge_specific_effect(acc, value):
    if isinstance(value, dict):
        acc = acc if isinstance(acc, dict) else {}
        acc.update(value)
    elif isinstance(value, list):
        acc = acc if isinstance(acc, list) else []
        for v in value:
            if v not in acc:
                acc.append(v)
    return acc


def fetch_language(lang: str, workers: int) -> dict:
    first = fetch_page(lang, 0)
    total = first["count"]
    step = len(first["sort_card_id_list"])
    if step == 0:
        raise RuntimeError(f"lang={lang} 首页为空")
    offsets = list(range(step, total, step))
    print(f"[{lang}] 共 {total} 张，每页 {step}，需再拉 {len(offsets)} 页", flush=True)

    with ThreadPoolExecutor(max_workers=workers) as pool:
        rest = list(pool.map(lambda o: fetch_page(lang, o), offsets))

    merged = {k: {} for k in DICT_KEYS}
    merged["sort_card_id_list"] = []
    merged["specific_effect_card_info"] = None
    merged["stats_list"] = first.get("stats_list")

    for page in [first] + rest:
        for k in DICT_KEYS:
            merged[k].update(page.get(k) or {})
        for cid in page["sort_card_id_list"]:
            if cid not in merged["sort_card_id_list"]:
                merged["sort_card_id_list"].append(cid)
        merged["specific_effect_card_info"] = merge_specific_effect(
            merged["specific_effect_card_info"], page.get("specific_effect_card_info"))

    listed = len(merged["sort_card_id_list"])
    missing = [c for c in merged["sort_card_id_list"] if str(c) not in merged["card_details"]]
    if listed != total or missing:
        raise RuntimeError(f"lang={lang} 校验失败: count={total} 实际={listed} 缺详情={missing[:5]}")

    merged["count"] = total
    print(f"[{lang}] 完成: {listed} 张（含衍生卡详情共 {len(merged['card_details'])} 条）", flush=True)
    return merged


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--langs", nargs="+", default=["chs", "cht"])
    ap.add_argument("--out", default="data")
    ap.add_argument("--workers", type=int, default=4)
    ap.add_argument("--no-proxy", action="store_true", help="忽略系统代理直连")
    args = ap.parse_args()

    if args.no_proxy:
        global OPENER
        OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    for lang in args.langs:
        data = fetch_language(lang, args.workers)
        path = out / f"cards_{lang}.json"
        path.write_text(
            json.dumps(data, ensure_ascii=False, separators=(",", ":"), sort_keys=True),
            encoding="utf-8", newline="\n")
        print(f"[{lang}] 已写入 {path} ({path.stat().st_size // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
