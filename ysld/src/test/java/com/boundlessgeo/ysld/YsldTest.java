package com.boundlessgeo.ysld;


import com.boundlessgeo.ysld.encode.YsldEncoder;
import com.boundlessgeo.ysld.parse.YsldParser;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.*;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;

public class YsldTest {

    @Test
    public void testParsePoint() throws Exception {
        StyledLayerDescriptor sld = parse("point.yml");
        dump(sld);
    }

    @Test
    public void testEncodePoint() throws Exception {
        YsldEncoder encoder = new YsldEncoder(new OutputStreamWriter(System.out));
        encoder.encode(load("point.sld"));
    }

    StyledLayerDescriptor load(String filename) throws IOException {
        SLDParser p = new SLDParser(CommonFactoryFinder.getStyleFactory());
        p.setInput(getClass().getResourceAsStream(filename));
        return p.parseSLD();
    }

    StyledLayerDescriptor parse(String filename) throws IOException {
        YsldParser parser = new YsldParser(getClass().getResourceAsStream(filename));
        return parser.parse();
    }

    void dump(StyledLayerDescriptor sld) throws Exception {
        SLDTransformer encoder = new SLDTransformer();
        encoder.setIndentation(2);
        encoder.transform(sld, System.out);
    }
}
