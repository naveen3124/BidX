package com.bengaluru.rss;

//Java NIO for file paths
import java.nio.file.Path;

//Lucene Core imports
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

//Lucene Analysis imports (Requires lucene-analysis-common)
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

//Lucene Document and Field imports
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

public final class LuceneIndexer {

    private final IndexWriter writer;

    public LuceneIndexer(Path indexDir) throws Exception {
        Directory dir = FSDirectory.open(indexDir);
        Analyzer analyzer = new StandardAnalyzer();

        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setRAMBufferSizeMB(256);
        cfg.setCommitOnClose(false);

        this.writer = new IndexWriter(dir, cfg);
    }

    public void index(FeedEntry e) throws Exception {
        Document d = new Document();
        d.add(new StringField("id", e.id(), Field.Store.YES));
        d.add(new StringField("source", e.source(), Field.Store.YES));
        d.add(new TextField("title", e.title(), Field.Store.YES));
        d.add(new TextField("summary", e.summary(), Field.Store.NO));
        d.add(new StringField("link", e.link(), Field.Store.YES));
        d.add(new LongPoint("published", e.publishedAt().toEpochMilli()));

        writer.updateDocument(new Term("id", e.id()), d);
    }

    public void commit() throws Exception {
        writer.commit();
    }
}
