/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.android.emma;

import com.vladium.emma.data.*;
import com.vladium.emma.report.*;
import com.vladium.util.IntObjectMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

public class AndroidEmmaProcessor {

    public static final String META_DATA_SUFFIX = ".em";
    public static final String COVERAGE_DATA_SUFFIX = ".ec";
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidEmmaProcessor.class);
    private final PropertiesBuilder<Integer, Integer> lineHitsBuilder = new PropertiesBuilder<Integer, Integer>(CoreMetrics.COVERAGE_LINE_HITS_DATA);

    private final SensorContext context;
    private final IReportDataModel model;
    private final JavaResourceLocator javaResourceLocator;

    public AndroidEmmaProcessor(File buildDir, JavaResourceLocator javaResourceLocator, SensorContext context) {
        try {
            ICoverageData coverageData = mergeCoverageData(buildDir);
            IMetaData metaData = mergeMetadata(buildDir);
            this.model = IReportDataModel.Factory.create(metaData, coverageData);
            this.context = context;
            this.javaResourceLocator = javaResourceLocator;
        } catch (IOException e) {
            throw new SonarException(e);
        }
    }

    private IMetaData mergeMetadata(File buildDir) throws IOException {
        // Merge all files with meta-data extension
        IMetaData metaData = DataFactory.newMetaData(CoverageOptionsFactory.create(new Properties()));
        File[] metaDataFiles = buildDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(META_DATA_SUFFIX);
            }
        });
        if (metaDataFiles != null && metaDataFiles.length > 0) {
            for (File metaDataFile : metaDataFiles) {
                IMergeable[] mergeableMetadata = DataFactory.load(metaDataFile);
                metaData.merge(mergeableMetadata[DataFactory.TYPE_METADATA]);
            }
        } else {
            LOGGER.warn("No metadata (*.em) file found in {}", buildDir);
        }
        return metaData;
    }

    private ICoverageData mergeCoverageData(File buildDir) throws IOException {
        // Merge all files with coverage extension
        ICoverageData coverageData = DataFactory.newCoverageData();
        File[] coverageDataFiles = buildDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(COVERAGE_DATA_SUFFIX);
            }
        });
        if (coverageDataFiles != null && coverageDataFiles.length > 0) {
            for (File coverageDataFile : coverageDataFiles) {
                IMergeable[] mergeableCoverageData = DataFactory.load(coverageDataFile);
                coverageData.merge(mergeableCoverageData[DataFactory.TYPE_COVERAGEDATA]);
            }
        } else {
            LOGGER.warn("No coverage (*.ec) file found in {}", buildDir);
        }
        return coverageData;
    }

    public void process() {
        MyVisitor myVisitor = new MyVisitor();
        model.getView(IReportDataView.HIER_SRC_VIEW).getRoot().accept(myVisitor, null);
        LOGGER.warn("Matched files in report : {}%", myVisitor.getMatchedPercentage());
        if (!myVisitor.missedClass.isEmpty()) {
            LOGGER.warn("{} files in emma report did not match any file in SonarQube Index : {}", myVisitor.filesUnmatched, myVisitor.missedClass);
        }
    }

    class MyVisitor extends AbstractItemVisitor {
        private int filesUnmatched = 0;
        private int totalFiles = 0;
        private String missedClass = "";

        @Override
        public Object visit(AllItem item, Object o) {
            work(item, o);
            return o;
        }

        @Override
        public Object visit(PackageItem item, Object o) {
            work(item, o);
            return o;
        }

        @Override
        public Object visit(SrcFileItem item, Object o) {
            totalFiles++;
            lineHitsBuilder.clear();
            int lines = 0;
            int coveredLines = 0;

            IntObjectMap map = item.getLineCoverage();
            for (int lineId : map.keys()) {
                SrcFileItem.LineCoverageData lineCoverageData = (SrcFileItem.LineCoverageData) map.get(lineId);
                lines++;
                final int fakeHits;
                if (lineCoverageData.m_coverageStatus == SrcFileItem.LineCoverageData.LINE_COVERAGE_COMPLETE) {
                    coveredLines++;
                    fakeHits = 1;
                } else {
                    fakeHits = 0;
                }
                lineHitsBuilder.add(lineId, fakeHits);
            }

            String packageName = item.getParent().getName();
            String className = packageName + "." + StringUtils.substringBeforeLast(item.getName(), ".");
            Resource resource = javaResourceLocator.findResourceByClassName(className);
            if (resource == null) {
                LOGGER.warn("Class with name : {} was not found in SonarQube index", className);
                missedClass += className + ", ";
                filesUnmatched++;
            } else {
                context.saveMeasure(resource, CoreMetrics.LINES_TO_COVER, (double) lines);
                context.saveMeasure(resource, CoreMetrics.UNCOVERED_LINES, (double) lines - coveredLines);
                context.saveMeasure(resource, lineHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));
            }

            return o;
        }

        private void work(IItem item, Object ctx) {
            Iterator iter = item.getChildren();
            while (iter.hasNext()) {
                IItem child = (IItem) iter.next();
                child.accept(this, ctx);
            }
        }

        public int getMatchedPercentage() {
            if (totalFiles == 0) {
                return 0;
            }
            return (totalFiles - filesUnmatched) * 100 / totalFiles;
        }

    }
}
