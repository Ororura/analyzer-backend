package com.ororura.analyzer.vacancy.infrastructure.hh;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

final class HhTextCleaner {

    private HhTextCleaner() {
    }

    static String clean(String html) {
        if (html == null || html.isBlank()) return null;
        Document document = Jsoup.parseBodyFragment(html);
        document.select("br").before(new TextNode("\n"));
        document.select("p, li").after(new TextNode("\n"));
        return document.body().wholeText()
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();
    }
}
