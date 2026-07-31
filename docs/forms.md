# Forms and dialogs

## Forms

`FormBuilder` lays out label-above-field forms on a grid, which is what keeps
gaps, label styling and helper text identical everywhere. Helper text sits under
the field it explains and states the rule up front — "3-20 characters", "Leave
blank to keep the current password" — rather than waiting for a failed submit.

Validation follows one pattern throughout:

1. The form parses only what it must, such as a year that is not a number, since
   that has to be reported before the value can reach a service at all.
2. Everything else goes to the service, which raises `ValidationException`
   carrying one message per problem.
3. `Dialogs.showValidationProblems` renders those as a bulleted list, so all
   complaints appear at once.

Messages name the fix, not the failure: "Password must be at least 8 characters"
rather than "invalid password".

## Dialogs

Four kinds, all through `Dialogs`, so tone and layout stay consistent:

- **Info and success** — confirm what happened, naming the record involved.
- **Warning** — something is blocked and explains what must happen first.
- **Confirm** — a question with the action itself as the button label. "Return"
  and "Remove", never "OK".
- **Destructive confirm** — the same, but defaulted to Cancel, so a stray Return
  keypress cannot delete a record.

Every message is escaped and wrapped in a fixed-width HTML label so long titles
break sensibly instead of stretching the dialog across the screen.
