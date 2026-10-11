APP = ./credit_simulator
MOCK_PORT = 8080
TARBALL = ../bcadigital-submission.tgz

.PHONY: help build verify test run run-file mock run-remote tarball docker-build clean

help: ## Tampilkan semua target.
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | sed 's/:.*## / - /'

build: ## Build jar cepat (tanpa test).
	mvn -q -DskipTests package

verify: ## Build penuh: test + gate coverage 85%.
	mvn verify

test: ## Jalankan 71 unit test saja.
	mvn test

run: build ## Jalankan interaktif.
	$(APP)

run-file: build ## Jalankan batch dari file_inputs.txt.
	$(APP) file_inputs.txt

mock: ## Serve mock lokal loan.json di :8080 (Ctrl-C untuk berhenti).
	@echo "Lalu di terminal lain: ./credit_simulator --url http://127.0.0.1:$(MOCK_PORT)/loan.json"
	python3 -m http.server $(MOCK_PORT) --directory mock-server

run-remote: build ## Jalankan interaktif lawan mock lokal (butuh 'make mock').
	$(APP) --url http://127.0.0.1:$(MOCK_PORT)/loan.json

tarball: ## Paket submission (.git ikut, build/PDF tidak).
	tar -czf $(TARBALL) \
		--exclude='./target' \
		--exclude='.DS_Store' \
		--exclude='TechTest Backend.pdf' \
		--exclude='.idea' \
		--exclude='.vscode' .
	@echo "OK: $(TARBALL)"; tar -tzf $(TARBALL) | head -5

docker-build: ## Build image Docker.
	docker build -t credit-simulator .

clean: ## Hapus output build.
	mvn -q clean
