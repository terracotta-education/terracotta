# Terracotta PII helpers: piiencrypt, piidecrypt, piihashemail
#
# Each one prompts for the Terracotta jar and the keys once, then keeps prompting for values until
# you type "quit", printing the result for each. The keys are only passed to the java command that
# does the work: they aren't exported, and they don't end up in your shell history.
#
# If PII_ENCRYPTION_KEY, PII_HASH_KEY or PII_ENCRYPTION_KEY_ID are already set in your shell,
# pressing Enter at that prompt uses the value that's set.

# prompts for a key without echoing it; Enter keeps the one already set in the shell, if any
_pii_prompt_key() {
    local name="$1" current="$2" typed

    if [ -n "$current" ]; then
        printf '%s (press Enter to use the one already set): ' "$name" >&2
    else
        printf '%s: ' "$name" >&2
    fi

    IFS= read -rs typed
    echo >&2
    printf '%s' "${typed:-$current}"
}

_pii_run() {
    local command="$1" jar key hash_key key_id value

    # a jar file, or a directory holding one (the newest terracotta*.jar in it is used)
    echo "Terracotta jar, or the directory it's in. Press Enter for $PWD/target" >&2
    read -rep "Jar: " jar
    jar="${jar:-$PWD/target}"

    if [ -d "$jar" ]; then
        jar="$(ls -t "$jar"/terracotta*.jar 2>/dev/null | head -1)"
    fi

    if [ ! -f "$jar" ]; then
        echo "No Terracotta jar found there." >&2
        return 2
    fi

    echo "Using $jar" >&2

    echo "Enter the keys Terracotta uses (nothing you type is shown)." >&2
    key="$(_pii_prompt_key PII_ENCRYPTION_KEY "$PII_ENCRYPTION_KEY")"
    hash_key="$(_pii_prompt_key PII_HASH_KEY "$PII_HASH_KEY")"

    if [ -z "$key" ] || [ -z "$hash_key" ]; then
        echo "Both keys are needed." >&2
        return 2
    fi

    printf 'PII_ENCRYPTION_KEY_ID (press Enter for %s): ' "${PII_ENCRYPTION_KEY_ID:-the default, 1}" >&2
    IFS= read -r key_id
    key_id="${key_id:-${PII_ENCRYPTION_KEY_ID:-1}}"

    echo >&2
    echo "Enter a value at each prompt. Type quit to finish." >&2

    while true; do
        printf 'Value: ' >&2

        # secrets to encrypt aren't shown as you type; stored values and emails are
        if [ "$command" = encrypt ]; then
            IFS= read -rs value || break
            echo >&2
        else
            IFS= read -r value || break
        fi

        [ "$value" = quit ] && break
        [ -z "$value" ] && continue

        # the value goes in on standard input, never as a command-line argument
        printf '%s\n' "$value" | PII_ENCRYPTION_KEY="$key" PII_HASH_KEY="$hash_key" PII_ENCRYPTION_KEY_ID="$key_id" \
            java -cp "$jar" -Dloader.main=edu.iu.terracotta.security.pii.PiiCli \
            org.springframework.boot.loader.launch.PropertiesLauncher "$command"

        # 2 means a key is missing or invalid, which every later value would hit too
        if [ $? -eq 2 ]; then
            return 2
        fi
    done
}

piiencrypt() { _pii_run encrypt; }
piidecrypt() { _pii_run decrypt; }
piihashemail() { _pii_run hash-email; }
alias ppihashemail=piihashemail
