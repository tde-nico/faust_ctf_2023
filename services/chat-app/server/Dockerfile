FROM faust.cs.fau.de:5000/chatapp-deps

COPY ./src app

ENTRYPOINT ["./venv/bin/gunicorn", "--capture-output", "--chdir", "app", "--log-level", "DEBUG", "--bind", "[::]:5000", "main:app"]