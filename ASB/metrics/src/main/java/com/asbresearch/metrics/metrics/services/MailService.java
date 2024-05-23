package com.asbresearch.metrics.metrics.services;

import com.asbresearch.metrics.metrics.models.bigquery.DailyStrategiesMI;
import com.asbresearch.metrics.metrics.models.bigquery.DailySupervisoryMI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MailService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private JavaMailSender mailSender;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private static final String date = dateFormat.format(new Date());

    @SuppressWarnings("unchecked")
    public void sendMail(DailySupervisoryMI dailySupervisoryMI, HashMap<String, List<DailyStrategiesMI>> dailyStrategiesMIMap) throws MessagingException {

        // Prepare the evaluation context
        final Context ctx = new Context(Locale.UK);
        ctx.setVariable("message", "hello world!");

        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message =
                new MimeMessageHelper(mimeMessage, true, "UTF-8"); // true = multipart
        message.setSubject("Pulse Daily Trading Metrics - " + date);
        message.setFrom("metrics@asbresearch.com");

        String[] to = {
                "claudiopaolicelli1@gmail.com",
                "fdr@asbresearch.com",
                "zhanel.karpykova@gmail.com",
                "pulsealerts_prod@asbresearch.com",
                "carloalbertobarranco@gmail.com",
                "yayodele@gmail.com",
        };

        message.setTo(to);

        String metricsSupervisory =
                "balance: " + dailySupervisoryMI.getBalance() + "\n" +
                "returnOnCapital(%): " + dailySupervisoryMI.getReturnOnCapital() + " \n" +
                "returnOnInvestment(%): " + dailySupervisoryMI.getReturnOnInvestment() + " \n" +
                "balanceUtilisationRate(%): " + dailySupervisoryMI.getBalanceUtilisationRate() + " \n" +
                "totalNetProfit: " + dailySupervisoryMI.getTotalNetProfit() + " \n" +
                "totalGrossProfit: " + dailySupervisoryMI.getTotalGrossProfit() + " \n" +
                "grossProfit: " + dailySupervisoryMI.getGrossProfit() + " \n" +
                "grossLoss: " + dailySupervisoryMI.getGrossLoss() + " \n" +
                "grossProfitFactor: " + dailySupervisoryMI.getGrossProfit() + " \n" +
                "tradesNumber: " + dailySupervisoryMI.getTradesNumber() + " \n" +
                "tradesProfitableRate(%): " + dailySupervisoryMI.getTradesProfitableRate() + " \n" +
                "tradesWinning: " + dailySupervisoryMI.getTradesWinning() + " \n" +
                "tradesLosing: " + dailySupervisoryMI.getTradesLosing() + " \n" +
                "averageTradeNetProfit: " + dailySupervisoryMI.getAverageTradeNetProfit() + " \n" +
                "ordersNumber: " + dailySupervisoryMI.getOrdersNumber() + " \n" +
                "ordersFailedRate(%): " + dailySupervisoryMI.getOrdersFailedRate() + " \n" +
                "ordersFullyMatchedRate(%): " + dailySupervisoryMI.getOrdersFullyMatchedRate() + " \n" +
                "ordersBestMatchedRate(%): " + dailySupervisoryMI.getOrdersBestMatchedRate() + " \n" +
                "ordersWorstMatchedRate(%): " + dailySupervisoryMI.getOrdersWorstMatchedRate() + " \n" +
                "ordersPartiallyMatchedRate(%): " + dailySupervisoryMI.getOrdersPartiallyMatchedRate() + " \n" +
                "ordersInPlayRate(%): " + dailySupervisoryMI.getOrdersInPlayRate() + " \n" +
                "eventsAvailable: " + dailySupervisoryMI.getEventsAvailable() + " \n" +
                "eventsTradedRate(%): " + dailySupervisoryMI.getEventsTradedRate() + " \n" +
                "largestWinningTradeProfit: " + dailySupervisoryMI.getLargestWinningTradeProfit() + " \n" +
                "largestLosingTradeLoss: " + dailySupervisoryMI.getLargestLosingTradeLoss() + " \n";

        StringBuilder metricsStrategies = new StringBuilder();

        for (Map.Entry me : dailyStrategiesMIMap.entrySet()) {

            List<DailyStrategiesMI> list = (List<DailyStrategiesMI>) me.getValue();

            metricsStrategies.append(me.getKey()).append("\n");

            for (DailyStrategiesMI dailyStrategiesMI : list) {

                String metric = "tradesProfitableRate".equals(dailyStrategiesMI.getMetric()) ||
                        "eventsTradedRate".equals(dailyStrategiesMI.getMetric())
                ? dailyStrategiesMI.getMetric() + "(%)" : dailyStrategiesMI.getMetric();

                metricsStrategies.append(metric)
                        .append(":")
                        .append(dailyStrategiesMI.getMetricValue())
                        .append("\n");
            }

            metricsStrategies.append("\n\n");
        }

        // Create the HTML body using Thymeleaf
        message.setText("Pulse Daily Trading Metrics - " + date +
                        " \n\n" + metricsSupervisory +
                        "\n\n" + metricsStrategies +
                "\n\nFor information contact claudio.paolicelli@asbresearch.com", false); // true = isHtml

        // Send mail
        this.mailSender.send(mimeMessage);
    }
}
