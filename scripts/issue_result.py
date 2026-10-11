#!/usr/bin/env python3
"""Comment the result of a merged pull request on every issue it closes. Run by .github/workflows/issue_result.yml.

    python scripts/issue_result.py <pull request number>

The comment holds the Done and Pictures sections of the description and folds the other sections. A hidden
mark with the pull request number finds the comment again, so a second run updates it instead of adding one.
Needs gh, and GH_REPO outside a checkout.
"""
import json
import re
import subprocess
import sys

SHOWN = ("## Done", "## Pictures")
AUTHOR = "github-actions[bot]"


def gh(*args, stdin=None):
    return subprocess.run(["gh", *args], input=stdin, stdout=subprocess.PIPE, encoding="utf-8", check=True).stdout


def mark(number):
    return f"<!-- pr-result:{number} -->"


def split_sections(description):
    """The text before the first `## ` heading, then one text per heading. A heading inside a code block is text."""
    sections, fence = [[]], None
    for line in description.replace("\r\n", "\n").split("\n"):
        ticks = re.match(r" {0,3}(`{3,}|~{3,})", line)
        if ticks and fence is None:
            fence = ticks[1]
        elif ticks and ticks[1].startswith(fence) and line.strip() == ticks[1]:
            fence = None
        elif fence is None and line.startswith("## "):
            sections.append([])
        sections[-1].append(line)
    return ["\n".join(s).strip() for s in sections]


def comment_body(number, description):
    # Text before the first section is the `Fixes` line. A description without the template is shown whole.
    sections = split_sections(description)
    shown = [s for s in sections if s.startswith(SHOWN)] or sections
    folded = [s for s in sections[1:] if s not in shown]
    parts = [mark(number), f"The result of #{number}:", *shown]
    if folded:
        parts += ["<details>\n<summary>The rest of the description</summary>", *folded, "</details>"]
    return "\n\n".join(parts) + "\n"


def main(number):
    pr = json.loads(gh("pr", "view", number, "--json", "body,closingIssuesReferences"))
    body = json.dumps({"body": comment_body(number, pr["body"])})
    for issue in pr["closingIssuesReferences"]:
        comments = f"repos/{{owner}}/{{repo}}/issues/{issue['number']}/comments"
        mine = f'.[] | select(.user.login == "{AUTHOR}" and (.body | contains("{mark(number)}"))) | .id'
        old = gh("api", comments, "--paginate", "--jq", mine).split()
        target = ["PATCH", f"repos/{{owner}}/{{repo}}/issues/comments/{old[0]}"] if old else ["POST", comments]
        gh("api", "-X", *target, "--input", "-", stdin=body)
        print(f"#{issue['number']}: {'updated' if old else 'added'} the comment")


if __name__ == "__main__":
    main(sys.argv[1])
