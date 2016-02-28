@Grapes(
  @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.21')
)
import java.text.SimpleDateFormat
import java.text.DateFormat
import groovy.time.TimeCategory

TimeZone utcTimeZone = TimeZone.getTimeZone("UTC")
isoDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmSS'Z'")
isoDateFormat.setTimeZone(utcTimeZone)

TimeZone berlinTimeZone = TimeZone.getTimeZone("Europe/Berlin")
berlinDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmSS")
berlinDateFormat.setTimeZone(berlinTimeZone)

def parser = new org.cyberneko.html.parsers.SAXParser()
def page = new XmlSlurper(parser).parse('http://hvbrandenburg-handball.liga.nu/cgi-bin/WebObjects/nuLigaHBDE.woa/wa/teamPortrait?teamtable=1459848&pageState=vorrunde&championship=HVBrandenburg+15%2F16&group=197383')

def table = page.'**'.find {
  it.'@id'=='content-row2'
}.TABLE.TBODY

def games = []

table.TR.each { row ->
  def game = [:]
  def isValidGame = false

  row.children().eachWithIndex { entry, i ->
  	if (entry.name() != "TH") {
  		isValidGame = true
	    switch (i) {
	    	case 1 : game.date = entry.text().trim(); break;
	    	case 2 : game.time = entry.text().trim(); game.time = game.time.size() > 5 ? game.time.substring(0, 5) : game.time; game.description = entry.@title; break;
	    	case 3 : game.location = "${entry.text().trim()}: ${entry.SPAN.@title}"; break;
	    	case 4 : game.number = entry.text().trim(); break;
	    	case 5 : game.home = entry.text().trim(); break;
	    	case 6 : game.guest = entry.text().trim(); break;
	    }
  	}
  }

	if (isValidGame && (game.home.contains("spielfrei") || game.guest.contains("spielfrei"))) {
		isValidGame = false
	}

  if (isValidGame) {
	  game.start = convertTime("${game.date} ${game.time}", false)
	  game.end = convertTime("${game.date} ${game.time}", true)

  	games.add(game)
  }
}

println games[10].date
println games[10].time
println games[10].location
println games[10].number
println games[10].home
println games[10].guest
println games[10].start
println games[10].end
println games[10].description

def convertTime(time, isEnd) {
	Date date = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(time)

	if (isEnd) {
		Integer.metaClass.mixin TimeCategory
		Date.metaClass.mixin TimeCategory
		date = date + 2.hours
	}

	String berlinTime = berlinDateFormat.format(date)
}

def now = isoDateFormat.format(new Date())

new File('Landesliga_Mitte_2015_16.ics').withWriter { out ->
	out.write("BEGIN:VCALENDAR\r\n")
	out.write("VERSION:2.0\r\n")
	out.write("PRODID:-//Ingmar Rötzler//NONSGML nuLiga Calendar//EN\r\n")
	out.write("METHOD:PUBLISH\r\n")
	out.write("X-WR-TIMEZONE:Europe/Berlin\r\n")
	out.write("CALSCALE:GREGORIAN\r\n")

	out.write("BEGIN:VTIMEZONE\r\n")
	out.write("TZID:Europe/Berlin\r\n")
	out.write("BEGIN:DAYLIGHT\r\n")
	out.write("TZOFFSETFROM:+0100\r\n")
	out.write("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n")
	out.write("DTSTART:19810329T020000\r\n")
	out.write("TZNAME:GMT+2\r\n")
	out.write("TZOFFSETTO:+0200\r\n")
	out.write("END:DAYLIGHT\r\n")
	out.write("BEGIN:STANDARD\r\n")
	out.write("TZOFFSETFROM:+0200\r\n")
	out.write("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n")
	out.write("DTSTART:19961027T030000\r\n")
	out.write("TZNAME:GMT+1\r\n")
	out.write("TZOFFSETTO:+0100\r\n")
	out.write("END:STANDARD\r\n")
	out.write("END:VTIMEZONE\r\n")

	games.each { game ->
		out.write("BEGIN:VEVENT\r\n")
		out.write("UID:${game.number}\r\n")
		out.write("LOCATION:${game.location}\r\n")
		out.write("SUMMARY:${game.home} - ${game.guest}\r\n")
		out.write("DESCRIPTION:${game.description}\r\n")
		out.write("CLASS:PUBLIC\r\n")
		out.write("DTSTART;TZID=Europe/Berlin:${game.start}\r\n")
		out.write("DTEND;TZID=Europe/Berlin:${game.end}\r\n")
		out.write("DTSTAMP:${now}\r\n")
		out.write("END:VEVENT\r\n")
	}

	out.write("END:VCALENDAR\r\n")
}
