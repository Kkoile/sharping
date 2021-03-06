package net.seesharpsoft.commons.util;

import java.text.ParseException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer<T>
{
    protected static class Token<T>
    {
        public final Pattern regex;
        public final T token;

        public Token(T token, Pattern regex)
        {
            Objects.requireNonNull(token, "token must be not null!");
            Objects.requireNonNull(regex, "regex must be not null!");
            this.regex = regex;
            this.token = token;
        }

        @Override
        public String toString() {
            return String.format("%s=%s (%s)", token, regex.pattern(), regex.flags());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Tokenizer.Token)) {
                return false;
            }
            Token otherToken = (Token)other;
            return Objects.equals(this.token, otherToken.token) &&
                    Objects.equals(this.regex.pattern(), otherToken.regex.pattern()) &&
                    Objects.equals(this.regex.flags(), otherToken.regex.flags());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.token, this.regex.pattern(), this.regex.flags());
        }
    }

    public static class TokenInfo<T>
    {
        public final T token;
        public final String sequence;

        public TokenInfo(T token, String sequence)
        {
            this.token = token;
            this.sequence = sequence;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", this.token, this.sequence);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Tokenizer.TokenInfo)) {
                return false;
            }
            TokenInfo otherTokenInfo = (TokenInfo)other;
            return Objects.equals(this.token, otherTokenInfo.token) && Objects.equals(this.sequence, otherTokenInfo.sequence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.token, this.sequence);
        }
    }

    private Map<T, Token<T>> tokenMap;
    private Pattern trimPatternStart;
    private Pattern trimPatternEnd;
    private boolean caseSensitive;

    public Tokenizer()
    {
        tokenMap = new HashMap<>();
        setCaseSensitive(true);
        setTrimPattern("\r| ");
    }

    public boolean getCaseSensitive() {
        return this.caseSensitive;
    }

    public Tokenizer<T> setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        return this;
    }

    protected Tokenizer<T> add(Token<T> token) {
        tokenMap.put(token.token, token);
        return this;
    }

    protected Token<T> createToken(T token, String regex, boolean caseSensitive) {
        return new Token(token, Pattern.compile("^(" + regex + ")", caseSensitive ? 0 : Pattern.CASE_INSENSITIVE));
    }

    public Tokenizer<T> add(T token, String regex, boolean caseSensitive) {
        return add(createToken(token, regex, caseSensitive));
    }

    public Tokenizer<T> add(T token, String regex) {
        return this.add(token, regex, getCaseSensitive());
    }

    protected Token<T> getToken(T token) {
        return tokenMap.get(token);
    }

    public Tokenizer<T> setTrimPattern(String regexTrimPattern) {
        if (regexTrimPattern == null || regexTrimPattern.isEmpty()) {
            this.trimPatternStart = null;
            this.trimPatternEnd = null;
        } else {
            this.trimPatternStart = Pattern.compile(String.format("^(%s)*", regexTrimPattern), getCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
            this.trimPatternEnd = Pattern.compile(String.format("(%s)*$", regexTrimPattern), getCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
        }
        return this;
    }

    protected String trim(String input) {
        if (input == null) {
            return "";
        }

        if (trimPatternStart != null) {
            input = trimPatternStart.matcher(input).replaceFirst("");
        }
        if (trimPatternEnd != null) {
            input = trimPatternEnd.matcher(input).replaceFirst("");
        }

        return input;
    }

    protected <T> List<TokenInfo<T>> tokenize(String str, Collection<Token<T>> tokenCollection, BiFunction<T, String, Boolean> matcherCallback) throws ParseException {
        List<TokenInfo<T>> tokenInfos = new LinkedList<>();
        String trimmedString = trim(str == null ? null : str.replaceAll("\r\n", "\n"));
        while (!trimmedString.equals(""))
        {
            boolean match = false;
            for (Token<T> info : tokenCollection)
            {
                Matcher matcher = info.regex.matcher(trimmedString);
                if (matcher.find())
                {
                    String sequence = trim(matcher.group());
                    if (matcherCallback == null || matcherCallback.apply(info.token, sequence)) {
                        match = true;
                        trimmedString = trim(matcher.replaceFirst(""));
                        tokenInfos.add(new TokenInfo(info.token, sequence));
                        break;
                    }
                }
            }
            if (!match) {
                throw new ParseException("Unexpected character in input: " + trimmedString, 0);
            }
        }
        return tokenInfos;
    }

    public List<TokenInfo<T>> tokenize(String str, BiFunction<T, String, Boolean> matcherCallback) throws ParseException {
        return tokenize(str, tokenMap.values(), matcherCallback);
    }

    public List<TokenInfo<T>> tokenize(String str) throws ParseException {
        return tokenize(str, tokenMap.values(), null);
    }
}

