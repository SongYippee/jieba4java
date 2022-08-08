package com.huaban.analysis.jieba;

import junit.framework.TestCase;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class JiebaSegmenterTest extends TestCase {

    @Test
    public void testInitUserDict() throws URISyntaxException {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        URI uri = Objects.requireNonNull(segmenter.getClass().getResource("/user_dicts/address")).toURI();
        segmenter.initUserDict(Paths.get(uri));
        List<String> tokens = segmenter.sentenceProcess("你们有没有去过新疆塔克拉玛大沙漠");
        tokens.forEach(System.out::println);
    }
}