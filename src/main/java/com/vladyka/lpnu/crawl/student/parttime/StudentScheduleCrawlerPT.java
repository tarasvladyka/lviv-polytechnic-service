package com.vladyka.lpnu.crawl.student.parttime;

import com.vladyka.lpnu.crawl.ScheduleCrawler;
import com.vladyka.lpnu.dto.ParsedScheduleEntry;
import com.vladyka.lpnu.exception.SchedulePageParseException;
import com.vladyka.lpnu.model.DateBasedScheduleEntry;
import com.vladyka.lpnu.model.Group;
import com.vladyka.lpnu.model.Institute;
import com.vladyka.lpnu.service.GroupService;
import com.vladyka.lpnu.service.InstituteService;
import com.vladyka.lpnu.service.impl.ParseServiceImpl;
import com.vladyka.lpnu.service.impl.ScheduleEntryServiceImpl;
import com.vladyka.lpnu.tools.Helper;
import com.vladyka.lpnu.tools.ParseUrlProvider;
import com.vladyka.lpnu.tools.ScheduleEntryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

import static com.vladyka.lpnu.model.enums.GroupType.STUDENT;
import static com.vladyka.lpnu.model.enums.ParseMode.DEMO;
import static com.vladyka.lpnu.model.enums.StudyForm.PART_TIME;

@Service
public class StudentScheduleCrawlerPT implements ScheduleCrawler {

    private Logger logger = LogManager.getLogger(getClass().getName());

    @Autowired
    private ParseServiceImpl parseService;
    @Autowired
    private InstituteService instituteService;
    @Autowired
    private ParseUrlProvider urlProvider;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ScheduleEntryBuilder scheduleEntryBuilder;
    @Autowired
    private ScheduleEntryServiceImpl scheduleEntryService;
    @Autowired
    private StudentInstitutesCrawlerPT institutesCrawlerPT;
    @Autowired
    private StudentGroupsCrawlerPT groupsCrawlerPT;
    @Autowired
    private Helper helper;

    @Value("${mode}")
    private String parseMode;


    @Override
    public void crawl() {
        institutesCrawlerPT.crawl();
        groupsCrawlerPT.crawl();
        logger.info("[Student schedule, Part-time] - Started crawling schedules");
        int total = groupService.findCount(STUDENT, PART_TIME);
        int counter = 0;
        for (Institute institute : instituteService.findAll()) {
            for (Group group : groupService.findAll(institute.getId(), STUDENT, PART_TIME)) {
                crawlSinglePage(institute.getAbbr(), group.getAbbr(), total, counter);
                counter++;
                if (DEMO.name().equalsIgnoreCase(parseMode)) {
                    break;
                }
            }
            if (DEMO.name().equalsIgnoreCase(parseMode)) {
                break;
            }
        }
        logger.info("[Student schedule, Part-time] - Finished crawling schedules, total = {}", total);
    }

    private void crawlSinglePage(String instAbbr, String groupAbbr, int total, int counter) {
        Long startTime = System.currentTimeMillis();
        Group targetGroup = groupService.find(groupAbbr, instAbbr, PART_TIME, STUDENT);
        String urlToParse = urlProvider.getGroupScheduleUrlPT(instAbbr, groupAbbr);
        try {
            List<ParsedScheduleEntry> parsedEntries = parseService.parseGroupSchedule(urlToParse);
            List<DateBasedScheduleEntry> resultEntries = new LinkedList<>();
            parsedEntries.forEach(parsed ->
                    resultEntries.addAll(scheduleEntryBuilder.buildDateBased(parsed, targetGroup)));
            scheduleEntryService.createAllDateBased(resultEntries);
        } catch (Exception e) {
            throw new SchedulePageParseException(urlToParse, e.getMessage());
        }
        Long endTime = System.currentTimeMillis();
        counter++;
        helper.printProgressLogs(groupAbbr, instAbbr, startTime, endTime, counter, total, urlToParse);
    }
}