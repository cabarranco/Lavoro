export const quickviewSiteData = {
    navigation: {
        quickview: "Quickview",
        betstore: "Betstore", // split betstore and log out to a seperate globalSiteData object file
        logOut: "Log out",
    },
    venues: [
        "Venue 1",
        "Venue 2",
        "Venue 3",
    ],
    portfolio: {
        title: "My portfolio",
        balance: "Balance",
        openPosition: "Open position",
        roc: "ROC (%)",
        qarbs: "Qarbs",
        dailyPL: "Daily P/L",
        bets: "Bets",
        qarbsWinLoss: "Qarbs wins/loss",
    },
    bets: {
        title: "Bets",
        headers: [
            "Event",
            "Strat ID",
            "Status",
            "Events",
            "Market outcome",
            "Order type",
            "Order side",
            "Order allocation",
        ],
        events: [
            {
                name: "0123456",
                stratId: "6543210",
                status: "Matched",
                events: "Football",
                marketOutcome: "1",
                orderType: "Bet",
                orderSide: "Lay",
                orderAllocation: "123456",
            },
            {
                name: "0123456",
                stratId: "6543210",
                status: "Matched",
                events: "Football",
                marketOutcome: "1",
                orderType: "Bet",
                orderSide: "Lay",
                orderAllocation: "123456",
            },
        ],
    },
    myAccounts: {
        title: "My accounts",
        headers: [
            "Venue",
            "Balance",
            "Open",
            "Accounts",
        ],
        accounts: [
            {
                venue: "VenueName",
                balance: "Balance",
                open: "£1,000,000",
                accounts: "1",
            },
            {
                venue: "VenueName",
                balance: "Balance",
                open: "£1,000,000",
                accounts: "1",
            },
        ],
    },
    qarbs: {
        title: "Qarbs"
    },
    netCapital: {
        title: "Trends",
        timeframe: [
            "1d",
            "5d",
            "1w",
            "1y",
            "All",
        ],
    },
    grossCapital: {
        title: "Trends",
        timeframe: [
            "1d",
            "5d",
            "1w",
            "1y",
            "All",
        ],
    },
    profitLoss: {
        title: "Trends",
        timeframe: [
            "1d",
            "5d",
            "1w",
            "1y",
            "All",
        ],
    },
}

export default quickviewSiteData;