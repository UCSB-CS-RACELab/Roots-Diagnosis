package edu.ucsb.cs.roots.scheduling;

import org.quartz.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class RootsJob implements Job {

    public static final String SCHEDULED_ITEM_INSTANCE = "scheduled-item-instance";

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        ScheduledItem scheduledItem = (ScheduledItem) jobDataMap.get(SCHEDULED_ITEM_INSTANCE);
        checkNotNull(scheduledItem);
        scheduledItem.run(context.getScheduledFireTime().getTime());
    }

}
