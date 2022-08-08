package com.huaban.analysis.jieba;

import com.huaban.analysis.jieba.word.PartOfSpeech;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TFIDFAnalyzer {

    HashMap<String, Double> idfMap;
    public HashSet<String> stopWordsSet;
    double idfMedian;

    private TFIDFAnalyzer() {
        if (stopWordsSet == null) {
            stopWordsSet = new HashSet<>();
            System.out.println("加载自定义 停用词，BEFORE, " + stopWordsSet.size() + " ,path=" + TFIDFAnalyzer.class.getResource("/stop_words.txt").getFile());
            loadStopWords(stopWordsSet, TFIDFAnalyzer.class.getResourceAsStream("/stop_words.txt"));
            System.out.println("加载自定义 停用词，AFTER, " + stopWordsSet.size());
        }
        if (idfMap == null) {
            idfMap = new HashMap<>();
            System.out.println("加载自定义 idf词库，BEFORE, " + idfMap.size() + ", path=" + TFIDFAnalyzer.class.getResource("/idf_dict.txt").getFile());
            loadIDFMap(idfMap, TFIDFAnalyzer.class.getResourceAsStream("/idf_dict.txt"));
            System.out.println("加载自定义 idf词库，AFTER, " + idfMap.size());
        }
    }

    public static TFIDFAnalyzer getInstance() {
        return Inner.instance;
    }

    private static class Inner {
        private static final TFIDFAnalyzer instance = new TFIDFAnalyzer();

    }


    /**
     * tfidf分析方法
     *
     * @param content 需要分析的文本/文档内容
     * @param topN    需要返回的tfidf值最高的N个关键词，若超过content本身含有的词语上限数目，则默认返回全部
     * @return
     */
    public List<Keyword> analyze(String content, int topN) {

        List<Keyword> keywordList = new ArrayList<>();
        Map<String, Double> tfMap = getTF(content);
        for (String word : tfMap.keySet()) {
            // 若该词不在idf文档中，则使用平均的idf值(可能定期需要对新出现的网络词语进行纳入)
            if (idfMap.containsKey(word)) {
                keywordList.add(new Keyword(word, idfMap.get(word) * tfMap.get(word)));
            } else
                keywordList.add(new Keyword(word, idfMedian * tfMap.get(word)));
        }

        Collections.sort(keywordList);

        if (keywordList.size() > topN) {
            int num = keywordList.size() - topN;
            for (int i = 0; i < num; i++) {
                keywordList.remove(topN);
            }
        }
        return keywordList;
    }

    /**
     * tf值计算公式
     * tf=N(i,j)/(sum(N(k,j) for all k))
     * N(i,j)表示词语Ni在该文档d（content）中出现的频率，sum(N(k,j))代表所有词语在文档d中出现的频率之和
     *
     * @param content
     * @return
     */
    private Map<String, Double> getTF(String content) {
        Map<String, Double> tfMap = new HashMap<>();
        if (StringUtils.isEmpty(content))
            return tfMap;

        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<String> segments = segmenter.sentenceProcess(content);
        Map<String, Integer> freqMap = new HashMap<>();
        HashMap<String, PartOfSpeech> wordsOfPOS = getWordsOfPOS(content);
        int wordSum = 0;
        for (String segment : segments) {
            //停用词不予考虑，单字词不予考虑
            if (!stopWordsSet.contains(segment) && segment.length() > 1) {
                PartOfSpeech pos = wordsOfPOS.get(segment);
                if (!Objects.isNull(pos) && (PartOfSpeech.isNoun(pos) || PartOfSpeech.isVerb(pos))){
                    wordSum++;
                    if (freqMap.containsKey(segment)) {
                        freqMap.put(segment, freqMap.get(segment) + 1);
                    } else {
                        freqMap.put(segment, 1);
                    }
                }
            }
        }

        // 计算double型的tf值
        for (String word : freqMap.keySet()) {
            tfMap.put(word, freqMap.get(word) * 0.1 / wordSum);
        }

        return tfMap;
    }

    private HashMap<String, PartOfSpeech> getWordsOfPOS(String content) {
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> segTokenList = segmenter.process(content, JiebaSegmenter.SegMode.INDEX, true);
        HashMap<String, PartOfSpeech> ans = new HashMap<>();
        for (SegToken x : segTokenList) {
            ans.put(x.word, x.partOfSpeech);
        }
        return ans;
    }

    /**
     * 默认jieba分词的停词表
     * url:https://github.com/yanyiwu/nodejieba/blob/master/dict/stop_words.utf8
     *
     * @param set
     */
    private void loadStopWords(Set<String> set, InputStream in) {
        BufferedReader bufr;
        try {
            bufr = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = bufr.readLine()) != null) {
                set.add(line.trim());
            }
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * idf值本来需要语料库来自己按照公式进行计算，不过jieba分词已经提供了一份很好的idf字典，所以默认直接使用jieba分词的idf字典
     * url:https://raw.githubusercontent.com/yanyiwu/nodejieba/master/dict/idf.utf8
     */
    private void loadIDFMap(Map<String, Double> map, InputStream in) {
        BufferedReader bufr;
        try {
            bufr = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = bufr.readLine()) != null) {
                String[] kv = line.trim().split(" ");
                map.put(kv[0], Double.parseDouble(kv[1]));
            }
            try {
                bufr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 计算idf值的中位数
            List<Double> idfList = new ArrayList<>(map.values());
            Collections.sort(idfList);
            idfMedian = idfList.get(idfList.size() / 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String content = "做核酸检查吗";
        int topN = 5;
        TFIDFAnalyzer tfidfAnalyzer = new TFIDFAnalyzer();
        List<Keyword> list = tfidfAnalyzer.analyze(content, topN);
        for (Keyword word : list)
            System.out.print(word.getName() + ":" + word.getTfidfvalue() + ",");
    }
}
