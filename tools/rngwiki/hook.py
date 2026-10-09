"""MkDocs hook entry point for the RNGTech wiki templates (see docs/reference/wiki-authoring.md)."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from rngwiki.site import WikiSite  # noqa: E402

_site: WikiSite | None = None


def on_config(config):
    global _site
    _site = WikiSite(config)


def on_files(files, config):
    _site.scan(files)


def on_page_markdown(markdown, page, config, files):
    return _site.render_page(markdown, page)


def on_post_build(config):
    _site.write_icons(config.site_dir)
