package com.asb.analytics;

import com.asb.analytics.bigquery.BigQueryServices;
import com.asb.analytics.bigquery.Row;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BetfairCerberusMapFunction {

	private static Gson gson = new GsonBuilder().create();

	public static void main(String[] args) {

		ExecuteShellCommand com = new ExecuteShellCommand();
		String token = com.executeCommand("gcloud auth print-access-token");

		List<Row> values = new ArrayList<>();

		competitionStaticMap.forEach((k,v) -> values.add(new Row(new CompetitionDictionary(k, v))));

		BigQueryServices.staticTables()
				.insertBigQuery(gson, values, token, "competition_dictionary");

		values.clear();

		teamStaticMap.forEach((k,v) -> values.add(new Row(new TeamDictionary(k, v))));

		BigQueryServices.staticTables()
				.insertBigQuery(gson, values, token, "team_dictionary");
	}
	
	private final static Map<Long, Long> competitionStaticMap = new HashMap<Long, Long>() {{
		put(10932509L, 2L); //Premier
		put(59L, 3L); //Bundesliga
		put(81L, 1L); //Serie A Ita
		put(117L, 4L); //Liga Spa
		put(55L, 5L); //Ligue 1 Fra
		put(9404054L, 19L); //Eredivisie
		put(99L, 15L); //Portogallo Primeira Liga
		put(7129730L, 18L); //Championship ENG
		put(11068551L, 27L); //Norway Eliteserien
		put(129L, 28L); //Allsveskan
		put(879931L, 29L); //China Super League
	}};

	private final static Map<Integer, Integer> teamStaticMap = new HashMap<Integer, Integer>() {{
		//United Kingdom
		put(1141, 61);
		put(1096, 35);
		put(63908, 41);
		put(1117, 273);
		put(48759, 279);
		put(48350, 280);
		put(64964, 274);
		put(48322, 277);
		put(78864, 276);
		put(18567, 59);
		put(69745, 272);
		put(56299, 57);
		put(205084, 269);
		put(48349, 278);
		put(55190, 36);
		put(52689, 50);
		put(48325, 270);
		put(56343, 45);
		put(56764, 42);
		put(78984, 63);
		put(70385, 62);
		put(48324, 261);
		put(48317, 281);
		put(48461, 52);
		put(56323, 38);
		put(47999, 47);
		put(48351, 37);
		put(63907, 44);
		put(56036, 265);
		put(893582, 271);
		put(25422, 39);
		put(47998, 48);
		put(69718, 267);
		put(56295, 266);
		put(62684, 58);
		put(103122, 264);
		put(18565, 268);
		put(48470, 263);
		put(47992, 275);
		put(58943, 43);
		put(69720, 55);
		put(62783, 53);
		put(48224, 46);
		put(56301, 56);
		put(1703, 49);
		put(48756, 54);
		put(69746, 262);
		put(48044, 51);
		put(79343, 33);
		put(62683, 34);
		put(223467,409);
		put(74303, 44);
		put(62523, 50);
		//Germania
		put(498560, 92);
		put(5774350, 65);
		put(44785, 68);
		put(347774, 77);
		put(879210, 260);
		put(44520, 72);
		put(11438560, 76);
		put(197307, 74);
		put(44518, 75);
		put(44787, 67);
		put(84649, 73);
		put(121724, 258);
		put(5340398, 79);
		put(50335, 69);
		put(44519, 66);
		put(6555433, 64);
		put(44521, 71);
		put(50347, 403);
		put(208026, 93);
		put(44517, 69);
		put(44783, 78);
		put(208035, 104);
		put(64374, 76);
		//Serie A ITA
		put(127991, 18);
		put(215817, 2);
		put(199545, 27);
		put(60297, 3);
		put(44506, 26);
		put(60294, 5);
		put(676467, 4);
		put(924268, 10);
		put(60310, 11);
		put(63347, 22);
		put(2423, 20);
		put(56966, 23);
		put(676464, 8);
		put(191607, 21);
		put(56967, 19);
		put(60311, 25);
		put(2013140, 9);
		put(3307906, 16);
		put(44503, 7);
		put(215821, 24);
		put(522054, 17);
		put(501219, 13);
		put(522046,16);
		put(508827,412);
		put(7407,411);
		put(676465,12);
		//Primera Liga Spagnola
		put(60324, 99);
		put(9162, 84);
		put(41433, 85);
		put(55270, 82);
		put(44507, 90);
		put(66483, 95);
		put(247969, 106);
		put(59044, 94);
		put(248010, 101);
		put(968185, 30);
		put(2250353, 257);
		put(309109, 120);
		put(214865, 97);
		put(10779, 121);
		put(2426, 81);
		put(44508, 86);
		put(79323, 91);
		put(66183, 80);
		put(13362, 256);
		put(28191, 88);
		put(10542,404);
		put(2255452,40);
		put(10543,87);
		//Mangiaranocchie
		put(70467, 126);
		put(473963, 129);
		put(10501, 105);
		put(310510, 119);
		put(305995, 132);
		put(676422, 28);
		put(44801, 122);
		put(44793, 107);
		put(44790, 111);
		put(44794, 125);
		put(44795, 112);
		put(44797, 124);
		put(70468, 127);
		put(361706, 60);
		put(10774, 102);
		put(189829, 110);
		put(77586, 29);
		put(55271, 96);
		put(44798, 103);
		put(489720, 98);
		put(298233, 131);
		put(44799, 108);
		put(309689, 118);
		put(51404, 114);
		put(305969, 115);
		put(4864974, 128);
		put(7534982, 113);
		put(669348, 407);
		//Olanda
		put(315069, 282);
		put(2685, 283);
		put(9220660, 284);
		put(48453, 201);
		put(67164, 293);
		put(55264, 286);
		put(8524431, 290);
		put(3630179, 287);
		put(419132, 294);
		put(1328613, 187);
		put(8750569, 291);
		put(48451, 285);
		put(50349, 289);
		put(56363, 292);
		put(13441259, 202);
		put(48455, 288);
		put(3954225, 190);
		put(24931403,290);
		put(111703,200);
		put(419126,205);
		put(49058,218);
		put(7928242,183);
		//Portuganji
		put(48786, 189);
		put(48783, 173);
		put(10761, 179);
		put(311029, 204);
		put(505958, 259);
		put(505957, 206);
		put(46964, 178);
		put(191604, 181);
		put(203723, 175);
		put(505982, 211);
		put(48785, 172);
		put(495321, 177);
		put(48793, 208);
		put(48799, 176);
		put(2506293, 174);
		put(4240339, 242);
		put(50085, 185);
		put(321036, 194);
		put(48787,191);
		put(48784,255);
		put(5875376,410);
		//Svezia Allsveskan
		put(168731, 371);
		put(151472, 355);
		put(4638398, 357);
		put(232104, 369);
		put(30684, 351);
		put(780883, 360);
		put(151470, 368);
		put(30680, 374);
		put(10765, 363);
		put(30679, 365);
		put(764222, 372);
		put(30681, 361);
		put(30689, 367);
		put(30683, 356);
		put(503361, 352);
		put(130433, 366);
		put(767907, 370);
		put(780882, 358);
		put(42617, 373);
		put(502427, 354);
		put(174383,364);
		put(7188665,362);
		put(4638067,359);
		//Norvegia
		put(3038746, 331);
		put(373549, 346);
		put(31317, 341);
		put(152566, 334);
		put(2440424, 340);
		put(10770, 342);
		put(3782270, 335);
		put(31321, 345);
		put(10833201, 337);
		put(2440426, 344);
		put(55224, 338);
		put(199646, 330);
		put(778909, 332);
		put(778903, 350);
		put(31322, 348);
		put(132563,339);
		put(50045,343);
		put(168747,347);
		put(50046,336);
		put(31319,333);
		put(22623366,344);
		//China
		put(2955487, 380);
		put(10939168, 386);
		put(2899297, 385);
		put(9279854, 382);
		put(11483923, 379);
		put(5723376, 390);
		put(9211368, 376);
		put(12584280, 378);
		put(10718994, 375);
		put(8412228, 391);
		put(10864488, 389);
		put(9276641, 396);
		put(2329275, 384);
		put(1550828, 383);
		put(6988032, 395);
		put(4525687,391);
		put(12685189,398);
		put(10335751,397);
		put(10752240,377);
		put(3727828,381);
	}};

	static class TeamDictionary {
		private Integer betfairTeamId;
		private Integer cerberusTeamId;

		public TeamDictionary(Integer betfairTeamId, Integer cerberusTeamId) {
			this.betfairTeamId = betfairTeamId;
			this.cerberusTeamId = cerberusTeamId;
		}

		public Integer getBetfairTeamId() {
			return betfairTeamId;
		}

		public void setBetfairTeamId(Integer betfairTeamId) {
			this.betfairTeamId = betfairTeamId;
		}

		public Integer getCerberusTeamId() {
			return cerberusTeamId;
		}

		public void setCerberusTeamId(Integer cerberusTeamId) {
			this.cerberusTeamId = cerberusTeamId;
		}
	}

	static class CompetitionDictionary {
		private Long betfairCompetitionId;
		private Long cerberusCompetitionId;

		public CompetitionDictionary(Long betfairCompetitionId, Long cerberusCompetitionId) {
			this.betfairCompetitionId = betfairCompetitionId;
			this.cerberusCompetitionId = cerberusCompetitionId;
		}

		public Long getCerberusCompetitionId() {
			return cerberusCompetitionId;
		}

		public void setCerberusCompetitionId(Long cerberusCompetitionId) {
			this.cerberusCompetitionId = cerberusCompetitionId;
		}

		public Long getBetfairCompetitionId() {
			return betfairCompetitionId;
		}

		public void setBetfairCompetitionId(Long betfairCompetitionId) {
			this.betfairCompetitionId = betfairCompetitionId;
		}
	}
}
