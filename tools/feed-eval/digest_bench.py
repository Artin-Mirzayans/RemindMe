#!/usr/bin/env python3
"""Benchmark a model on the Today & Tomorrow digest task, scored against ESPN.

Builds the same prompt the app does (real rubric, real ESPN fixtures), runs it
through a local Ollama model and/or Claude, then checks every event the model
returns against the fixtures ESPN actually published:

  grounded   - matches a real fixture, with the exact UTC start time
  wrong_time - matches a real fixture's teams but the start time is off
  invented   - matches no fixture at all

Standard library only, so it runs on a bare machine.

  python3 digest_bench.py --model qwen2.5:3b
  python3 digest_bench.py --model qwen2.5:3b --claude          # needs ANTHROPIC_API_KEY
  python3 digest_bench.py --dry-run                            # just show the prompt size
  python3 digest_bench.py --fixtures fixtures.json --model llama3.2:3b   # same input every run
"""
import argparse
import json
import os
import re
import sys
import textwrap
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RUBRIC_PATH = ROOT / "server/src/main/java/com/remindme/digest/DigestPromptRubric.java"

ESPN_BASE = "https://site.api.espn.com/apis/site/v2/sports/"
LEAGUES = [
    "soccer/uefa.champions", "soccer/uefa.europa", "soccer/eng.1", "soccer/esp.1",
    "soccer/ita.1", "soccer/ger.1", "soccer/usa.1", "football/nfl", "football/college-football",
    "basketball/nba", "basketball/wnba", "hockey/nhl", "baseball/mlb", "racing/f1",
    "mma/ufc", "golf/pga",
]
NATIONAL_NETWORKS = ["espn", "fox", "fs1", "tbs", "tnt", "mlb network", "apple tv", "roku", "nbc",
                     "peacock", "abc", "cbs", "amazon", "prime", "netflix"]
ISO_UTC = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


# ---------------------------------------------------------------- ESPN fixtures

def fetch_scoreboard(league, date):
    req = urllib.request.Request(f"{ESPN_BASE}{league}/scoreboard?dates={date}",
                                 headers={"User-Agent": "curl/8.5.0"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return league, json.load(resp)
    except Exception:
        return league, None


def parse_scoreboard(root, league):
    competition = (root.get("leagues") or [{}])[0].get("name", league)
    national_only = league == "baseball/mlb"
    fixtures = []
    for event in root.get("events", []):
        if event.get("status", {}).get("type", {}).get("state") != "pre":
            continue
        names = []
        for comp in (event.get("competitions") or [{}])[:1]:
            for b in comp.get("broadcasts", []):
                names.extend(b.get("names", []))
        if national_only and not any(any(n in x.lower() for n in NATIONAL_NETWORKS) for x in names):
            continue
        start = event.get("date", "")
        if re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}Z", start):
            start = start[:16] + ":00Z"
        fixtures.append({"matchup": event.get("name", ""), "startsAt": start,
                         "competition": competition, "watch": ", ".join(names)})
    return fixtures


def fetch_tavily(query, key):
    try:
        data = post_json("https://api.tavily.com/search", {
            "query": query, "topic": "general", "search_depth": "basic",
            "max_results": 4, "include_answer": False,
        }, {"Content-Type": "application/json", "Authorization": f"Bearer {key}"}, 30)
    except Exception as e:
        print(f"tavily search failed: {e}")
        return "(search failed)"
    lines = [f"Query: {query}"]
    for r in data.get("results", []):
        lines.append(f"- {r.get('title', '')} ({r.get('url', '')}): {r.get('content', '')}")
    return "\n".join(lines) + "\n"


def get_research(now, cache_path, use_tavily):
    """Returns (now, fixtures, tv, other). Saved with the clock it was taken at, so every
    model - on any machine, any day - sees identical input."""
    today, tomorrow = now.date(), now.date() + timedelta(days=1)
    if cache_path and Path(cache_path).exists():
        cached = json.loads(Path(cache_path).read_text())
        if isinstance(cached, dict) and cached.get("now") and (not use_tavily or cached.get("tv")):
            print(f"using saved research from {cache_path}")
            pinned = datetime.strptime(cached["now"], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
            return pinned, cached["fixtures"], cached.get("tv"), cached.get("other")
    dates = [today.strftime("%Y%m%d"), tomorrow.strftime("%Y%m%d")]
    jobs = [(lg, d) for lg in LEAGUES for d in dates]
    seen, fixtures = set(), []
    with ThreadPoolExecutor(max_workers=16) as pool:
        for league, data in pool.map(lambda j: fetch_scoreboard(*j), jobs):
            if data is None:
                continue
            for f in parse_scoreboard(data, league):
                key = (f["matchup"], f["startsAt"])
                if key not in seen:
                    seen.add(key)
                    fixtures.append(f)
    tv = other = None
    if use_tavily:
        key = os.environ.get("TAVILY_API_KEY")
        if not key:
            sys.exit("set TAVILY_API_KEY to include the web research (or drop --tavily)")
        tv = fetch_tavily(f"big TV and streaming events on {today} and {tomorrow} - awards shows, major season "
                          "premieres and finales, live network specials - with air times", key)
        other = fetch_tavily(f"notable live event to watch online on {today} or {tomorrow} - a concert "
                             "livestream, a rocket launch, a big cultural broadcast", key)
    if cache_path:
        Path(cache_path).write_text(json.dumps({"now": now.strftime("%Y-%m-%dT%H:%M:%SZ"), "fixtures": fixtures,
                                                 "tv": tv, "other": other}, indent=2))
        print(f"saved research to {cache_path}")
    return now, fixtures, tv, other


# ------------------------------------------------------------------- the prompt

def load_rubric():
    if not RUBRIC_PATH.exists():
        sys.exit(f"can't find {RUBRIC_PATH} - run this from a full clone of the repo")
    src = RUBRIC_PATH.read_text()
    block = src.split('static final String TEXT = """', 1)[1].split('""";', 1)[0]
    block = textwrap.dedent(block.lstrip("\n"))
    return block.replace("\\\n", "")


def build_prompt(now, fixtures, tv=None, other=None):
    today = now.date()
    tomorrow = today + timedelta(days=1)
    sports = "\n".join(f"{f['matchup']} | {f['startsAt']} | {f['competition']}"
                       + (f" | {f['watch']}" if f["watch"] else "") for f in fixtures)
    research = (
        "SCHEDULED SPORTS (exact data - matchup | UTC start | competition | how to watch):\n"
        f"{sports}\n\nTV / STREAMING (web search - verify against your own knowledge):\n"
        f"{tv or '(not included in this benchmark)'}\n\nOTHER LIVE EVENTS (web search):\n"
        f"{other or '(not included in this benchmark)'}\n"
    )
    iso_now = now.strftime("%Y-%m-%dT%H:%M:%SZ")
    return (
        f"The current time is {iso_now} (UTC). Below is what's on for {today} and {tomorrow} (today and "
        "tomorrow). Pick the things worth watching live over that window, starting from the "
        "current time.\n\n" + research + "\n" + load_rubric() + "\n\n\n"
        "Respond with ONLY this JSON object, nothing before or after it:\n"
        f'{{"date":"{today}","events":[{{"title":"...","summary":"...","category":"...","rating":5,'
        '"startsAt":"yyyy-MM-ddTHH:mm:ssZ","howToWatch":"...","recommendedDescription":"...",'
        '"recommendedReminderAt":"yyyy-MM-ddTHH:mm:ssZ","source":"..."}]}\n'
    )


# --------------------------------------------------------------------- models

def post_json(url, body, headers, timeout):
    req = urllib.request.Request(url, data=json.dumps(body).encode(), headers=headers)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.load(resp)


def run_ollama(model, prompt, host, num_ctx, timeout):
    start = time.time()
    data = post_json(f"{host}/api/generate", {
        "model": model, "prompt": prompt, "stream": False, "format": "json",
        "options": {"temperature": 0, "num_ctx": num_ctx, "num_predict": 3000},
    }, {"Content-Type": "application/json"}, timeout)
    elapsed = time.time() - start
    pe, e = data.get("prompt_eval_count", 0), data.get("eval_count", 0)
    pe_s, e_s = data.get("prompt_eval_duration", 0) / 1e9, data.get("eval_duration", 0) / 1e9
    return data.get("response", ""), {
        "seconds": round(elapsed, 1), "prompt_tokens": pe, "output_tokens": e,
        "prompt_tok_per_s": round(pe / pe_s, 1) if pe_s else None,
        "output_tok_per_s": round(e / e_s, 1) if e_s else None, "cost_usd": 0.0,
    }


def run_claude(model, prompt, timeout):
    key = os.environ.get("ANTHROPIC_API_KEY")
    if not key:
        sys.exit("set ANTHROPIC_API_KEY to run the Claude baseline")
    start = time.time()
    data = post_json("https://api.anthropic.com/v1/messages", {
        "model": model, "max_tokens": 6000, "thinking": {"type": "disabled"},
        "messages": [{"role": "user", "content": prompt}],
    }, {"x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"}, timeout)
    elapsed = time.time() - start
    text = "".join(b.get("text", "") for b in data.get("content", []))
    u = data.get("usage", {})
    inp, out = u.get("input_tokens", 0), u.get("output_tokens", 0)
    rate_in, rate_out = (0.80, 4.00) if "haiku" in model else (3.00, 15.00)
    return text, {
        "seconds": round(elapsed, 1), "prompt_tokens": inp, "output_tokens": out,
        "prompt_tok_per_s": None, "output_tok_per_s": round(out / elapsed, 1) if elapsed else None,
        "cost_usd": round((inp * rate_in + out * rate_out) / 1e6, 4),
    }


# -------------------------------------------------------------------- scoring

# words too common to tell two teams apart ("Iowa State" vs "Washington State")
GENERIC = {"state", "university", "city", "united", "college", "saint", "football", "club", "the", "and"}


def tokens(text):
    return {w for w in re.findall(r"[a-z0-9]+", text.lower()) if len(w) >= 3 and w not in GENERIC}


def title_matches(title, matchup):
    """Does this event title refer to this fixture? Models shorten names ("BYU" for
    "BYU Cougars"), so each side of the fixture just needs one distinctive word in common."""
    t = tokens(title)
    sides = re.split(r"\s+(?:at|vs\.?|v)\s+", matchup, flags=re.I)
    if len(sides) == 2 and all(tokens(s) for s in sides):
        return all(tokens(s) & t for s in sides)
    return len(tokens(matchup) & t) >= 2


def parse_json(text):
    start, end = text.find("{"), text.rfind("}")
    if start < 0 or end <= start:
        return None
    try:
        return json.loads(text[start:end + 1])
    except json.JSONDecodeError:
        return None


def score(text, fixtures):
    parsed = parse_json(text)
    if parsed is None:
        return {"json_ok": False}
    events = parsed.get("events") or []
    counts = {"grounded": 0, "wrong_time": 0, "not_in_espn": 0}
    rating_ok = reminder_ok = desc_ok = time_fmt_ok = 0
    problems = []
    for ev in events:
        title, starts = str(ev.get("title", "")), str(ev.get("startsAt", ""))
        matches = [f for f in fixtures if title_matches(title, f["matchup"])]
        if any(f["startsAt"] == starts for f in matches):
            counts["grounded"] += 1
        elif matches:
            counts["wrong_time"] += 1
            problems.append(f"wrong time: {title} says {starts}, ESPN says {matches[0]['startsAt']}")
        else:
            counts["not_in_espn"] += 1
            problems.append(f"not in ESPN data: {title}")
        time_fmt_ok += bool(ISO_UTC.match(starts))
        rating_ok += ev.get("rating") in (3, 4, 5)
        remind = str(ev.get("recommendedReminderAt", ""))
        reminder_ok += bool(ISO_UTC.match(remind) and ISO_UTC.match(starts) and remind < starts)
        desc_ok += 0 < len(str(ev.get("recommendedDescription", ""))) <= 40
    n = len(events)
    return {"json_ok": True, "events": n, **counts, "time_format_ok": time_fmt_ok,
            "rating_ok": rating_ok, "reminder_ok": reminder_ok, "description_ok": desc_ok,
            "problems": problems[:8]}


def report(name, stats, result, web=False):
    print(f"\n=== {name} ===")
    print(f"time {stats['seconds']}s | prompt {stats['prompt_tokens']} tok"
          f" ({stats['prompt_tok_per_s']} tok/s) | output {stats['output_tokens']} tok"
          f" ({stats['output_tok_per_s']} tok/s) | cost ${stats['cost_usd']}")
    if not result["json_ok"]:
        print("RESULT: no parseable JSON - this alone would produce an empty feed")
        return
    n = result["events"] or 1
    print(f"events returned: {result['events']}")
    print(f"  grounded (real fixture, exact time): {result['grounded']}/{result['events']}")
    print(f"  wrong time:                          {result['wrong_time']}/{result['events']}")
    note = "  <- could be real TV/web events, check by eye" if web else "  <- nothing here should be unconfirmed"
    print(f"  not in ESPN data:                    {result['not_in_espn']}/{result['events']}{note}")
    print(f"  valid UTC format {result['time_format_ok']}/{n} | rating 3-5 {result['rating_ok']}/{n}"
          f" | reminder before start {result['reminder_ok']}/{n} | label <=40 chars {result['description_ok']}/{n}")
    for p in result["problems"]:
        print(f"  ! {p}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--model", action="append", default=[], help="Ollama model (repeatable)")
    ap.add_argument("--claude", action="store_true", help="also run Claude as the baseline")
    ap.add_argument("--claude-model", default="claude-sonnet-5")
    ap.add_argument("--host", default="http://localhost:11434")
    ap.add_argument("--num-ctx", type=int, default=16384)
    ap.add_argument("--timeout", type=int, default=3600)
    ap.add_argument("--fixtures", help="load research from this file, or fetch and save it there")
    ap.add_argument("--tavily", action="store_true", help="include the two web searches the app uses (needs TAVILY_API_KEY)")
    ap.add_argument("--dry-run", action="store_true", help="build the prompt and stop")
    ap.add_argument("--score-only", help="score a saved model reply instead of calling a model")
    ap.add_argument("--out", help="write all results to this JSON file")
    args = ap.parse_args()

    now = datetime.now(timezone.utc).replace(microsecond=0)
    now, fixtures, tv, other = get_research(now, args.fixtures, args.tavily)
    prompt = build_prompt(now, fixtures, tv, other)
    web = bool(tv)
    print(f"{len(fixtures)} real fixtures from ESPN | prompt is roughly {len(prompt) // 2} tokens")
    if not web:
        print("note: sports-only prompt. The app's real prompt also carries web search results and is "
              "larger (around 13k tokens), so speeds here are optimistic - add --tavily for the realistic size.")

    results = {}
    if args.score_only:
        report("saved reply", {"seconds": 0, "prompt_tokens": 0, "output_tokens": 0, "prompt_tok_per_s": None,
                               "output_tok_per_s": None, "cost_usd": 0}, score(Path(args.score_only).read_text(), fixtures), web)
        return
    if args.dry_run:
        print("\n--- prompt preview ---\n" + prompt[:1500] + "\n...")
        return
    if not args.model and not args.claude:
        sys.exit("nothing to run - pass --model <name> and/or --claude (or --dry-run)")

    runs = [(m, lambda m=m: run_ollama(m, prompt, args.host, args.num_ctx, args.timeout)) for m in args.model]
    if args.claude:
        runs.append((args.claude_model, lambda: run_claude(args.claude_model, prompt, args.timeout)))
    for name, run in runs:
        print(f"\nrunning {name} ...", flush=True)
        try:
            text, stats = run()
        except Exception as e:
            print(f"{name} failed: {e}")
            continue
        result = score(text, fixtures)
        report(name, stats, result, web)
        results[name] = {"stats": stats, "score": result, "reply": text}
    if args.out:
        Path(args.out).write_text(json.dumps(results, indent=2))
        print(f"\nsaved results to {args.out}")


if __name__ == "__main__":
    main()
