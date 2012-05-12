package wp.reverts.core;

import gnu.trove.map.TIntObjectMap;

public class RevertParser {

    private static class NameAndId {
        String name;
        int id;
    }

    NameAndId parseNameAndId(String s) {
        String tokens[] = s.split("@");
        if (tokens.length != 2) {
            throw new RuntimeException("illegal name and id: '" + s + "'");
        }
        NameAndId ni = new NameAndId();
        ni.id = Integer.valueOf(tokens[0]);
        ni.name = tokens[1];
        return ni;
    }

    long parseTstamp(String s) {
        return -1;
    }

    public Revert parse(String s, TIntObjectMap<User> users, TIntObjectMap<Article> articles) {
        String tokens[] = s.split("\t");
        if (tokens.length != 10) {
            return null;
        }
        NameAndId pair = parseNameAndId(tokens[0]);
        String code = tokens[1];
        Article article = articles.get(pair.id);
        if (article == null) {
            article = new Article(pair.id, pair.name);
            articles.put(pair.id, article);
        }

        int revertingId = Integer.valueOf(tokens[2]);
        long revertingTstamp= parseTstamp(tokens[3]);
        pair = parseNameAndId(tokens[4]);
        String revertingComment = tokens[5];
        User revertingUser = users.get(pair.id);
        if (revertingUser  == null) {
            revertingUser = new User(pair.id, pair.name);
            users.put(pair.id, revertingUser);
        }

        int revertedId = Integer.valueOf(tokens[6]);
        long revertedTstamp= parseTstamp(tokens[7]);
        pair = parseNameAndId(tokens[8]);
        String revertedComment = tokens[9];
        User revertedUser = users.get(pair.id);
        if (revertedUser  == null) {
            revertedUser = new User(pair.id, pair.name);
            users.put(pair.id, revertedUser);
        }

        Revert revert = new Revert(article, code, revertedUser, revertedTstamp, revertingUser, revertingTstamp);
        revert.setRevertedComment(revertedComment);
        revert.setRevertingComment(revertingComment);

        return revert;
    }
}
