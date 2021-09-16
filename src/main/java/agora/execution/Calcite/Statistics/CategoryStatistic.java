package agora.execution.Calcite.Statistics;

import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelDistribution;
import org.apache.calcite.rel.RelReferentialConstraint;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.util.ImmutableBitSet;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class CategoryStatistic implements Statistic {

    private final long rowCount;

    public CategoryStatistic(long rowCount) {
        this.rowCount = rowCount;
    }

    @Override
    public @Nullable Double getRowCount() {
        return (double) rowCount;
    }

    @Override
    public boolean isKey(ImmutableBitSet columns) {
        return false;
    }

    @Override
    public @Nullable List<ImmutableBitSet> getKeys() {
        return null;
    }

    @Override
    public @Nullable List<RelReferentialConstraint> getReferentialConstraints() {
        return null;
    }

    @Override
    public @Nullable List<RelCollation> getCollations() {
        return null;
    }

    @Override
    public @Nullable RelDistribution getDistribution() {
        return null;
    }
}
