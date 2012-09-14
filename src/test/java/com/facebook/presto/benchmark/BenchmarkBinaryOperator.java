package com.facebook.presto.benchmark;

import com.facebook.presto.aggregation.AverageAggregation;
import com.facebook.presto.block.BlockStream;
import com.facebook.presto.block.Cursor;
import com.facebook.presto.block.uncompressed.UncompressedBlockSerde;
import com.facebook.presto.operation.SubtractionOperation;
import com.facebook.presto.operator.GroupByBlockStream;
import com.facebook.presto.operator.HashAggregationBlockStream;
import com.facebook.presto.operator.UncompressedBinaryOperator;
import com.facebook.presto.slice.Slice;
import com.facebook.presto.slice.Slices;
import io.airlift.units.DataSize;
import io.airlift.units.Duration;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BenchmarkBinaryOperator
{
    public static void main(String[] args)
            throws IOException, InterruptedException
    {
        File column3 = new File("data/columns/column3.data");
        File column4 = new File("data/columns/column4.data");
        File column5 = new File("data/columns/column5.data");

        Slice column3Slice = Slices.mapFileReadOnly(column3);
        Slice column4Slice = Slices.mapFileReadOnly(column4);
        Slice column5Slice = Slices.mapFileReadOnly(column5);
        for (int i = 0; i < 100000; ++i) {
            BlockStream column3Stream = UncompressedBlockSerde.readAsStream(column3Slice);
            BlockStream column4Stream = UncompressedBlockSerde.readAsStream(column4Slice);
            BlockStream column5Stream = UncompressedBlockSerde.readAsStream(column5Slice);
            UncompressedBinaryOperator sub = new UncompressedBinaryOperator(column4Stream, column3Stream, new SubtractionOperation());

            GroupByBlockStream groupBy = new GroupByBlockStream(column5Stream);
            HashAggregationBlockStream aggregation = new HashAggregationBlockStream(groupBy, sub, AverageAggregation.PROVIDER);

            Result result = doIt(aggregation);
            long count = result.count;
            Duration duration = result.duration;

            DataSize fileSize = new DataSize(column3.length() + column4.length() + column5.length(), DataSize.Unit.BYTE);

            System.out.println(String.format("%s, %s, %.2f/s, %2.2f MB/s", duration, count, count / duration.toMillis() * 1000, fileSize.getValue(DataSize.Unit.MEGABYTE) / duration.convertTo(TimeUnit.SECONDS)));
        }
        Thread.sleep(1000);
    }

    public static Result doIt(BlockStream source)
    {
        long start = System.nanoTime();

        Cursor cursor = source.cursor();

        int count = 0;
        while (cursor.advanceNextPosition()) {
            ++count;
        }

        Duration duration = Duration.nanosSince(start);

        return new Result(count, duration);
    }

    public static class Result
    {
        private final int count;
        private final Duration duration;

        public Result(int count, Duration duration)
        {
            this.count = count;
            this.duration = duration;
        }
    }

}
