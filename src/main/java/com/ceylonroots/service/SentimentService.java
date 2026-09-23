package com.ceylonroots.service;

import com.ceylonroots.model.SentimentLabel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight keyword-based sentiment scoring, mirroring the logic used on the frontend prototype.
 * Not NLP-grade, but transparent, fast, and easy to defend in a viva: every matched keyword
 * that drove the label is returned alongside the score.
 */
@Service
public class SentimentService {

    private static final Set<String> POSITIVE_WORDS = Set.of(
            "excellent","great","fresh","aromatic","fragrant","fast","quick","smooth","reliable",
            "best","love","amazing","perfect","authentic","good","prompt","professional",
            "recommend","satisfied","superb","clean","well-packed","beautiful","premium","consistent"
    );

    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "late","delay","delayed","broken","bad","damaged","stale","poor","slow","rude",
            "terrible","worst","moldy","musty","missing","short","weak","disappointed",
            "complaint","refund","never","awful","cracked","wrong","overpriced"
    );

    public record SentimentResult(SentimentLabel label, int score, List<String> matchedKeywords) {}

    public SentimentResult analyze(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        List<String> matched = new ArrayList<>();
        int pos = 0, neg = 0;

        for (String w : POSITIVE_WORDS) {
            if (lower.contains(w)) { pos++; matched.add(w); }
        }
        for (String w : NEGATIVE_WORDS) {
            if (lower.contains(w)) { neg++; matched.add(w); }
        }

        int score = pos - neg;
        SentimentLabel label = score > 0 ? SentimentLabel.POSITIVE
                : score < 0 ? SentimentLabel.NEGATIVE
                : SentimentLabel.NEUTRAL;

        return new SentimentResult(label, score, matched);
    }
}
