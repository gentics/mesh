FROM jotschi/protractor

RUN mkdir -p /app
COPY *.js /app/

CMD ["protractor", "--seleniumAddress", "http://selenium:4444/wd/hub", "/app/protractor.conf.js"]
